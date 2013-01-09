---
layout: default
title: Bot #10 - Food Finder
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_09.html' data-next='/tutorial/tutorial_80_example_01.html' />

# Bot #10: Food Finder

## Objective

The bot for the first time examines its surroundings, searches for the nearest food item
(edible plant), runs towards it and eats it. To realize this, we need to examine the
string value the server passes to the control function in the "view" parameter. The string
looks something like this:

    WWWWWWWW_____WW_____WW__M__WW_____WW____PWWWWWWWW

Note that this is a simplified version; the actual string sent by the game server will
contain a larger view and will be be much longer. The size of the "view" string is described
in detail in the document **Scalatron Game Rules**.

The string has a very simple structure: it is a cell-by-cell rendering of the surroundings
of the bot into an ASCII string. Every letter in the string corresponds to a cell on the
game board. The view is always square and its edges span an odd number of cells.

The example string above contains 49 characters, leading us to deduce that it is a
rendering of a 7x7 square of cells. Each letter corresponds to a game entity. If we
break the view into multiple lines by fragmenting it at every 7th letter, we obtain:

    WWWWWWW
    W_____W
    W_____W
    W__M__W
    W_____W
    W____PW
    WWWWWWW

The game entity character codes are described in detail in the document **Scalatron Protocol**.
Here is an excerpt:

* "_" empty cell
* "W" wall
* "M" Bot (=master; yours, always in the center unless seen by a slave)
* "P" Zugar (=good plant, food)

So what the bot observes in the example is:

* the view is completely surrounded by wall blocks (`W`)
* the bot itself resides exactly at the center (`M`)
* at the bottom right there is an edible plant (`P`)

Given this string, the bot now needs to find the nearest food item and move towards it.
We'll again develop the code to achieve this in several steps.



## Step #1: a view parser class

Obviously, in a programming game in which bots must react to what they see in the game,
there'll be a lot of view parsing and analyzing. So we anticipate that we'll benefit from
having a smarter representation of the view string than a sequence of ASCII characters.
Since the view won't change while we work on it each cycle, a simple solution would be to
just wrap the string and add whatever fields and methods we may want:

    case class View(cells: String)

To access the cell at a particular index, we can provide an `apply()` method.

Like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_1.scala">Load into Editor</button>

    case class View(cells: String) {
        def apply(index: Int) = cells.charAt(index)
    }

We could use this code in our control function as follows:

    val viewString = paramMap("view")
    val view = View(viewString)
    val cell = view(10)



## Step #2: XY cell access

However, we'll want to mostly access cells using `XY` coordinates, via the 2D integer vector
class we just defined in the preceding chapter. To do that, we need a method that translates
`XY` coordinates into indices, like so:

    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size

We refer to this addressing scheme as using "absolute positions" since the `XY` value
is relative to the top-left corner of the view. We'll look at positioning relative to
the bot's location shortly.

However, the absolute position to index translation code relies on an as-yet-undefined
value `size`, which contains the number of cells per line. We could compute this on-the-fly
of course by defining a method `size`:

    def size = math.sqrt(cells.length).intValue

But since this method would be called *a lot* we probably want to cache the value. So we
could convert the definition from a method into an immutable field, which would be
initialized at construction time:

    val size = math.sqrt(cells.length).intValue

The `View` class now looks like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_2.scala">Load into Editor</button>

    case class View(cells: String) {
        val size = math.sqrt(cells.length).intValue
        def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
        def apply(absPos: XY) = cells.charAt(indexFromAbsPos(absPos))
    }

We can now construct and use `View` instances as follows:

    val viewString = paramMap("view")
    val view = View(viewString)
    val cell = view(XY(1,1))



### Detour: class parameter versus field

As a quick detour, let's examine an alternative implementation. Why not shift the `size`
field into the `case class` parameter list at the top and provide a factory method that
computes this value up front? Like so:

    object View {
        def apply(view: String): View = View(math.sqrt(view.length).toInt, view)
    }
    case class View(size: Int, cells: String) {
        def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
        def apply(absPos: XY) = cells.charAt(indexFromAbsPos(absPos))
    }

Note that here, the Scala compiler requires that we specify the return type of the `apply()`
method because the method is overloaded. How is it overloaded even though there is only one
variant here? Well, we defined `View` as a `case class`, so the compiler automatically
generates an `apply()` method for the parameters of the class at compile time, which is
not visible for us in the source code. For overloaded functions, Scala requires an
explicitly specified return type, which we therefore specify here. The reason we add another
`apply()` function is that it allows users of the `View` class to omit the `size` parameter
now expected by the auto-generated `apply()`.

But would this even be a good alternative? It would certainly work, but recalling that
using a `case class` also generates implementations of `toString`, `equals` and `hashCode`
for us that are based on the class parameter list, we'd now burden each one with the `size`
parameter which really is redundant: for equality or hashing we only need the string. So
while this solution is possible, it is here better to leave `size` as a field that is
defined within the class body, more like a class-local constant value.



## Step #3: relative addressing

Absolute addressing (relative to top-left of the view) is useful, but eventually we'll
always want to guide a bot to some location, and the game server expects all movement commands
to contain offsets relative to the bot position. So a very common use case is relative
addressing, where we supply and receive coordinates relative to the center of the view
(where the bot resides).

So we'll expand the `View` class with relative addressing, modify the `apply()` method to
expect relative `XY` addresses and add a few more utility methods.

Here is the enhanced `View` class:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_3.scala">Load into Editor</button>

    case class View(cells: String) {
        val size = math.sqrt(cells.length).toInt
        val center = XY(size/2, size/2)

        def apply(relPos: XY) = cellAtRelPos(relPos)

        def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
        def absPosFromIndex(index: Int) = XY(index % size, index / size)
        def absPosFromRelPos(relPos: XY) = relPos + center
        def cellAtAbsPos(absPos: XY) = cells.apply(indexFromAbsPos(absPos))

        def indexFromRelPos(relPos: XY) = indexFromAbsPos(absPosFromRelPos(relPos))
        def relPosFromAbsPos(absPos: XY) = absPos - center
        def relPosFromIndex(index: Int) = relPosFromAbsPos(absPosFromIndex(index))
        def cellAtRelPos(relPos: XY) = cells(indexFromRelPos(relPos))
    }




## Step #4: finding the nearest food item

Let's now add a method that the bot can use to find the nearest food item visible in its
view. In Java style, we might declare such a method with the signature

    def offsetToNearest(c: Char) : XY

where we pass the ASCII letter that we're looking for (here, `P`) as a parameter and expect
the relative `XY` coordinate as a result.

What if no cell of the desired type is presently visible within the view, however? In Java,
we might return `null` to indicate this special case, or, somewhat better, we might define
some instance of `XY` to act as a sentinel value. We might also throw an exception (not that
this would be a good choice here, since it is not really an unexpected occurrence).

But in none of these cases, what the function returns is obvious to a caller without
additional documentation. We might forget to handle a `null` value and get a
`NullPointerException`, or we might screw up the comparison to our sentinel value,
which in addition clutters our namespace.

Fortunately, Scala offers a much better approach, which is to return an `Option` value.
An `Option` is a generic abstract class whose instances are either `None` (a singleton)
or a `Some` value that wraps the actual result value. Using `Option`, we can change
the signature of our method to:

    def offsetToNearest(c: Char) : Option[XY]

All potential users of this method immediately know that the result may be either
`None` if no such character occurrence is found or `Some(xy)` if one is found. Using
`Option` is absolutely the preferred way to handle such cases in Scala and for this
reason most Scala programmers haven't seen a `NullPointerException` in a long time.

Adopting this signature, we can implement the method in a variety of ways. Here are
three options, just to give you an idea of the options available to you.



### Variant A: procedural style with `for`

Here is a version that is closest to the classic procedural style known from Java or C++
(note that this function should reside inside the View class definition):
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_4A.scala">Load into Editor</button>

    def offsetToNearest(c: Char) = {
        var nearestPosOpt : Option[XY] = None
        var nearestDistance = Double.MaxValue
        for(i <- 0 until cells.length) {
            if(c == cells(i)) {
                val pos = absPosFromIndex(i)
                val distanceToCenter = pos.distanceTo(center)
                if(distanceToCenter < nearestDistance) {
                    nearestDistance = distanceToCenter
                    nearestPosOpt = Some(pos - center)
                }
            }
        }
        nearestPosOpt
    }

The method uses a `for()` expression to scans all cell indices in a loop. The `for()` expression
works by iterating over elements drawn from a **range** (of integers) generated by the expression
`0 until cells.length`. It uses two mutable values to store the nearest cell found so far and
its distance to the bot.

Using constructs like `for()`, it is generally straight-forward to translate Java code into
Scala code. In fact, in IntelliJ IDEA, you can copy Java code from a Java source file and
paste it directly as Scala code into a Scala source file, with automatic translation.



### Variant B: functional style

However, for any such procedural implementation there is generally a more compact and more
elegant purely functional implementation. Having mutable values, doing manual range
comparisons and generally writing code that is longer than necessary introduces opportunities
for errors to sneak in. So here is an example of a functional implementation that gets by
without any mutable state:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_4B.scala">Load into Editor</button>

    def offsetToNearest(c: Char) = {
        val relativePositions =
            cells
            .view
            .zipWithIndex
            .filter(_._1 == c)
            .map(p => relPosFromIndex(p._2))
        if(relativePositions.isEmpty)
            None
        else
            Some(relativePositions.minBy(_.length))
    }

Let's take this apart:

* `cells.view` indicates that the subsequent call to `zipWithIndex` should not generate
  a new collection instance but rather a transient view onto the existing collection.
  This ensures that no new copy of the collection will be made.
* `.zipWithIndex` generates a collection of pairs in which each element of the
  original collection (the `Char` elements of our `cells` `String`) is paired with its
  index in the collection.
* `.filter(_._1 == c)` generates a collection that contains only those elements which
  match the filter predicate. The predicate we use is `_._1 == c`, i.e. whether the
  first element of the (just-zipped) pair equals the desired character value stored
  in `c`. Note there that the first underscore is an anonymous reference to the parameter
  passed to the predicate function. More verbosely, this could be written as
  `pair => { pair._1 == c}`.
* `.map(p => relPosFromIndex(p._2))` transforms the collection of pairs into a collection
  of `XY` instances representing the relative cell coordinates of the matching cells.
  This is achieved by supplying a transform function to match that receives a pair
  as its parameter (accessible via the parameter name `p`) and maps the second element
  of the pair (the index) to a relative position via an invocation of `relPosFromIndex()`.
* `if(relativePositions.isEmpty)` then tests whether the resulting collection is empty
  (i.e. there were no matches) and if so, returns `None`. If there were matches,
  it uses...
* `.minBy(_.length)` to find the one nearest to the center. This works by supplying a
  "scoring" function to `minBy` that, for each element in the collection, returns a value
  for which an ordering exists (here, an `Int`). The expression `_.length` is the usual
  short-hand employing an underscore as a reference to the (anonymous) call parameter.
  The verbose variant would be `relPos => { relPos.length }`.
* `Some(...)` finally wraps the result into a `Some` instance, matching expected the
  `Option[XY]` return type.



### Variant C: procedural style with `while`

So the functional style is very concise and for many reasons preferable for the vast
majority of code one might write. However, you may have noticed that a lot is going
on here, with several traversals and potential interim copies of the collection being
generated in calls like `zipWithIndex`. Some of this overhead can be eliminated or
minimized, for example by using `view` to avoid a copy.

But what about the 1% of your code where you really, absolutely require optimal
efficiency? The answer is that in those rare cases, you may want to write procedural
code with mutable state. And the good news is that you can do that. Here is a more
performance-conscious version of the `offsetToNearest` method that uses a `while`
loop to do an efficient traversal:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_4C.scala">Load into Editor</button>

    def offsetToNearest(c: Char) = {
        var nearestPosOpt : Option[XY] = None
        var nearestDistance = Double.MaxValue
        var i = 0
        while(i < cells.length) {
            if(c == cells(i)) {
                val pos = absPosFromIndex(i)
                val distanceToCenter = pos.distanceTo(center)
                if(distanceToCenter < nearestDistance) {
                    nearestDistance = distanceToCenter
                    nearestPosOpt = Some(pos - center)
                }
            }
            i += 1
        }
        nearestPosOpt
    }

The traversal with `while` will often be slightly faster than with `for` because
`for` will be translated by the compiler into functional code using closures, which
carry a slight overhead.

The takeaway is that initially, Scala allows you to start coding like you would in
Java, using familiar constructs like `for` loops. As you learn about functional
programming, Scala lets you write concise, error-minimizing functional code for the
99% of your code where productivity trumps performance. In the other 1% of your code
where performance trumps productivity, Scala also lets you write procedural code that
is as fast as anything you might write in Java.



### Variant D: but ... there is also parallelism

For *sequential* code, that is. If you expect to be able to exploit multiple CPU
cores, the picture changes again dramatically. Now, purely functional code that embraces
immutability gains an upper hand because it is so much more readily parallelizable.

Let's imagine for a moment that we were not processing a few hundred characters in
a bot view, as we expect to be doing here, but rather millions. What would it take
to parallelize this code to exploit multiple cores? For the procedural code, use
your own experience to judge the amount of work this would take.

For the functional version, here is the only change we'd have to make: we'd replace
the reference to `cells` at the top with `cells.par`, turning it into a parallel
collection. That's it.

Here is a real-life example from the Scalatron game server. The primary bottleneck for
the server is the drawing code, which uses unaccelerated Java drawing code. On an 8-core
system, the single-threaded variant of the server generates close to 100% CPU load, i.e.
it loads only a single CPU, as would be expected. After spending an hour or so to add
some hand-crafted threading code to introduce double buffering and parallelize drawing
and state updating, the CPU load rises to 180%. But then, after spending about ten seconds
to add a call to `.par` in the bot response generation code to parallelize all bot
control function invocations, the load rises to 300%.



## Step #5: exploiting the nearest food item

Now that we have the `offsetToNearest` method available, we can use it in our bot's control
function to guide out bot towards the nearest food items.

Like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_10_step_5.scala">Load into Editor</button>

    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val viewString = paramMap("view")
            val view = View(viewString)
            view.offsetToNearest('P') match {
                case Some(offset) =>
                    val unitOffset = offset.signum
                    "Move(direction=" + unitOffset + ")"
                case None =>
                    ""
            }
        } else ""
    }

Let's look at what this code does in more detail.



### What do `match` and `case` do?

In Java, we can handle situations in which a value can take on one of several possible
values with a `switch` statement. Scala provides pattern matching via `match` and `case`,
which is more general. To see how this works, let's isolate the associated example code:

    view.offsetToNearest('P') match {
        case Some(offset) => /* handler code */
        case None => /* handler code */
    }

The value to be examined precedes the `match` keyword. Here, we wish to examine the value
returned by `view.offsetToNearest('P')`, i.e. an `Option` value that may or may not contain
an `XY` instance. Within the curly braces that follow after the `match` keyword, we define
the cases to be handled. We do this by using the `case` keyword, followed by the pattern we
want to match against, followed by a `=>` symbol, followed by the handler code for that case.

So in the code above, since we know the result type of `offsetToNearest` to be an `Option[XY]`,
we have two handlers: one for the case where `None` is returned and one for the case where
`Some(offset)` is returned.

Like `if` or `try`, a `match` expression yields a value. It is the value associated with
the first matching handler. Here, the handler for `None` simply yields the empty string; the handler
for `Some(offset)` assembles a `Move` command that instructs the bot to move in the direction
of the nearest visible plant.

Some other notes on `match` and `case`:

* In Scala, unlike Java, there is no fall-through from one case to the next.
* The last pattern can be an underscore ('_'), which matches any pattern:

        foo match {
            // handlers for specific cases
            case _ => // handler for everything else
        }

* Patterns can be constants, like so:

        foo match {
            case "Frank" => // handler for "Frank"
            case "Joe" =>   // handler for "Joe"
            case "Tina" =>  // handler for "Tina"
        }

* Patterns can be types, with a symbol name to bind the value to if the type
  matches, like so:

        foo match {
            case n: Int => println("an Int: " + b)
            case s: String => println("a String: " + s)
            case c: Char => println("a Char: " + c)
        }

Caveat: in Scala, since it is based on and constrained by the Java Virtual Machine and its
generic type system, generic types are subject to type erasure just like they are in Java.
Due to this limitation, `case` patterns cannot distinguish between variants of a generic
type by subtype. Code like the following will therefore not work:

    anOption match {
        case a: Some[A] => // ...
        case b: Some[B] => // ...
    }

The only exception to this rule is `Array`, which is not subject to type erasure.



### What does `case Some(offset)` do?

The `match` expression we just examined contains one `case` handler that is neither
a constant nor (just) a type:

    ... match {
        case Some(offset) => // handler code
    }

Here, Scala *extracts* the value contained in the `Some` instance for us and binds it
to a symbol `offset`, which is of type `XY` (since the matched value was an `Option[XY]`).

This parameter extraction is available for classes whose companion object implements a
(static) method `unapply()`, which the compiler uses to extract the class parameters.
