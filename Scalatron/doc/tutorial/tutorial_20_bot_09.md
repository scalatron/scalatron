---
layout: default
title: Bot #9 - Missile Launcher II
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_08.html' data-next='/tutorial/tutorial_20_bot_10.html' />

# Bot #9: Missile Launcher II

## Objective

The missile launcher in the preceding section
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_08_sample_1.scala">Load into Editor</button>
was the second bot variant in which
we had to work with X/Y coordinates. Obviously, for a bot navigating a two-dimensional environment,
there'll be a lot more of this. So we will write a little class that makes dealing 
with such X/Y coordinates more convenient.

Departing from the style of the preceding examples for a moment we will not present
the entire source code up front but rather develop it in several steps.


## Step 1: using 'Tuple'

Since we already know about `Tuple` and constructing tuples with Scala is so
convenient, we might start out without introducing our own type and simply
work with `Tuple2`. The only code we have so far for working with X/Y coordinates
simply generates a random coordinate using an instance of a random number generator:

    val dx = rnd.nextInt(3)-1
    val dy = rnd.nextInt(3)-1
    val direction = dx + ":" + dy // e.g. "-1:1"
    "Spawn(direction=" + direction + ",heading=" + direction + ")"

Note that we're omitting the `energy` parameter; the server will then simply use the
minimum spawn-energy value (100 EU) as a default.

Let's define a method that constructs a tuple instead of two separate values:

    def random() = (rnd.nextInt(3)-1, rnd.nextInt(3)-1)

We could then rewrite our code as follows:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_1.scala">Load into Editor</button>

    def random() = (rnd.nextInt(3)-1, rnd.nextInt(3)-1)
    val xy = random()
    val direction = xy._1 + ":" + xy._2 // e.g. "-1:1"
    "Spawn(direction=" + direction + ",heading=" + direction + ")"




## Detour: parentheses on methods with no arguments

Just a very brief detour to talk about parentheses on invocations of methods that
take no parameters, like `random()` above: the convention in Scala is to omit the
parentheses if the method **has no** side effects, i.e. does not change any state (in
most cases this means it does nothing beyond computing and returning a value). If 
the method **has** side effects, we'll signal that by declaring and invoking it with 
empty parentheses, as we do above.
 
Why does the method have side effects, even though it looks like it just computes
and returns a value? Well, invoking `nextInt(n)` changes the internal state of
the randomizer, and this is a side effect of invoking our `random()` method. That
is why we declared it with parentheses.



## Step 2: moving construction to an `object`

If the construction of pairs holding random values would be accessed from several
call sites in our code, we'd have to supply the random number generator as a parameter
instead of silently accessing the one in the surrounding object instance.

Like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_2.scala">Load into Editor</button>

    def random(rnd: Random) = (rnd.nextInt(3)-1, rnd.nextInt(3)-1)

But now we have no reference to the `this` context at all, and the method could exist
as a static factory function. To get a static function, we need an `object`, so let's
define one:

    object XY {
        def random(rnd: Random) = (rnd.nextInt(3)-1, rnd.nextInt(3)-1)
    }

We can then update the code fragment within the `ControlFunction` to use this static
method, as follows:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_3.scala">Load into Editor</button>

    val xy = XY.random(rnd)
    val direction = xy._1 + ":" + xy._2 // e.g. "-1:1"
    "Spawn(direction=" + direction + ",heading=" + direction + ")"



## Step 3: creating our own class

Now what if we wanted to transform an X/Y coordinate instance, for example by adding
another X/Y value as an offset? We could extend `Tuple2` and add methods to it, but
that is inadvisable for both technical reasons (`Tuple2` is a `case class`, more on
those soon) and because we prefer composition over inheritance. We could also do some
fancy footwork and provide new methods for the existing `Tuple2` type (using Scala's
"pimp my library" approach), but that's not really called for if we expect to add a
lot of methods.

No, the natural path here is to define our own `class`, just like we would in Java:

    class XY(val x: Int, val y: Int)

which is roughly equivalent to the following Java code:

    class XY {
        final int x;
        final int y;
        
        XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

We can now instantiate values of this class like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_4.scala">Load into Editor</button>

    val xy = new XY(1, 1)



## Step 4: adding a member function

Let's add a method called `add` that will add the x and y fields of a given
`XY` instance to those of the current one:

    class XY(val x: Int, val y: Int) {
        def add(other: XY) = new XY(x+other.x, y+other.y)
    }

We can use this method as follows:

    val xy1 = new XY(1, 1)
    val xy2 = new XY(2, 2)
    val xy3 = xy1.add(xy2)

Keep in mind that we're striving for immutability, i.e. methods like `add()`
will always return a new instance rather than modifying the existing instance.
    
In Scala, we can call our method almost whatever we want, including the naturally
attractive `+`, so we'll use that:

    class XY(val x: Int, val y: Int) {
        def +(other: XY) = new XY(x+other.x, y+other.y)
    }

and use it as follows:

    val xy1 = new XY(1, 1)
    val xy2 = new XY(2, 2)
    val xy3 = xy1 + xy2

As a side note, the ability to create methods that look like operators leads a lot
of programmers to define methods that look like operators, resulting in libraries
that define lots of methods that look like operators and are consequently completely 
incomprehensible to anybody who does not know the operator symbols. Don't be put off
by this, though: soon enough you'll know all the operator symbols you frequently use
and will know how to look up the others. And you'll probably join the crowd of 
programmers defining lots of methods that look like operators, although an argument
could be made that code with short symbols is not necessarily better code.




## Step 5: creating a factory function

Let's assemble the code we have so far:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_5.scala">Load into Editor</button>

    class XY(val x: Int, val y: Int) {
        def +(other: XY) = new XY(x+other.x, y+other.y)
    }
    object XY {
        def random(rnd: Random) =
            new XY(rnd.nextInt(3)-1, rnd.nextInt(3)-1)
    }

We have a `class` definition with a `+` method and a companion `object` with a
`random()` factory function. Using the `new` keyword, we can construct class instances
and using the factory methods we can generate random instances. Now, we know of the
syntactic sugar we get with methods called `apply()`.

So why don't we add one:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_6.scala">Load into Editor</button>

    ...
    object XY {
        def apply(x: Int, y: Int) = new XY(x, y)
        def random(rnd: Random) =
            XY(rnd.nextInt(3)-1, rnd.nextInt(3)-1)
    }

Now, instead of littering our code with `new XY(x,y)` we can say just `XY(x, y)` to 
construct `XY` instances:

    val xy3 = XY(1, 1) + XY(2, 2) 




## Step 6: upgrading to a `case class`

Next, we'll "upgrade" our class definition to a `case class`:

    case class XY(x: Int, y: Int)  

The modifier keyword `case` before the `class` keyword basically instructs the compiler:
"add all the stuff to my class that a sane person will usually want". This includes:
 
* an immutable field for each parameter of the `class` definition  
* an `equals()` method that tests equality by checking each field 
* a `hashCode` method that takes into account each field 
* a `toString` method that takes into account each field 

as well as a few others. Upgrading a `class` to a `case class` also automatically 
generates a companion object of the same name with an `apply()` function as a 
factory function that takes the parameters of the `class` as its arguments.

The new code in its entirety looks like this:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_7.scala">Load into Editor</button>

    case class XY(x: Int, y: Int) {
        def +(other: XY) = XY(x+other.x, y+other.y)
    }
    object XY {
        def random(rnd: Random) = XY(rnd.nextInt(3)-1, rnd.nextInt(3)-1)
    }

Where our hand-coded `apply()` was removed from the definition of `object XY`
because the compiler now generates it automatically for us at compile time.

We can now do stuff like this:

    assert(XY(1,1) == XY(1,1))
    assert(XY(1,1) != XY(2,2))
    assert(XY(1,1).hashCode == XY(1,1).hashCode)
    assert(XY(1,1).toString == "XY(1,1)")
    assert(XY(1,1) + XY(2,2) = XY(3,3))


## Step 7: adding a few other methods

We can add methods like the following to our `class`:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_8.scala">Load into Editor</button>

    case class XY(x: Int, y: Int) {
        // ...
        def isNonZero = x!=0 || y!=0
        def isZero = x==0 && y==0
        def isNonNegative = x>=0 && y>=0
        
        def updateX(newX: Int) = XY(newX, y)
        def updateY(newY: Int) = XY(x, newY)
    
        def addToX(dx: Int) = XY(x+dx, y)
        def addToY(dy: Int) = XY(x, y+dy)
    
        def +(pos: XY) = XY(x+pos.x, y+pos.y)
        def -(pos: XY) = XY(x-pos.x, y-pos.y)
        def *(factor: Double) = XY((x*factor).intValue, (y*factor).intValue)

        def distanceTo(pos: XY) : Double = (this-pos).length
        def length : Double = math.sqrt(x*x + y*y)

        def signum = XY(x.signum, y.signum)

        def negate = XY(-x, -y)
        def negateX = XY(-x, y)
        def negateY = XY(x, -y)
    }

And we can expand out static code in `object` as well:

    object XY {
        val Zero = XY(0,0)
        val One =  XY(1,1)

        val Right      = XY( 1,  0)
        val RightUp    = XY( 1, -1)
        val Up         = XY( 0, -1)
        val UpLeft     = XY(-1, -1)
        val Left       = XY(-1,  0)
        val LeftDown   = XY(-1,  1)
        val Down       = XY( 0,  1)
        val DownRight  = XY( 1,  1)
    }


## Step 8: the modified bot

Using the `XY` class we just defined, we can update our bot's `respond()` method as follows:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_9.scala">Load into Editor</button>

    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val generation = paramMap("generation").toInt
            if( generation == 0 ) {
                if( paramMap("energy").toInt >= 100 && rnd.nextDouble() < 0.05 ) {
                    val heading = XY.random(rnd)
                    val headingStr = heading.x + ":" + heading.y // e.g. "-1:1"
                    "Spawn(direction=" + headingStr + ",heading=" + headingStr + ")"
                } else ""
            } else {
                val headingStr = paramMap("heading")
                val directions = headingStr.split(':').map(_.toInt) // e.g. "-1:1" => Array(-1,1)
                "Move(direction=" + directions(0) + ":" + directions(1) + ")"
            }
        } else ""
    }

We notice that we could also shift the encoding and decoding of the `heading` state parameter
into the `XY` class:

    case class XY(x: Int, y: Int) {
        // ...
        override def toString = x + ":" + y
    }

    object XY {
        // ...
        def fromString(s: String) = {
            val xy = s.split(':').map(_.toInt) // e.g. "-1:1" => Array(-1,1)
            XY(xy(0), xy(1))
        }
    }

Note the `override` modifier on the definition of `toString`. In Scala, overriding a method
that is already defined in a base class or trait requires this modifier in order to keep
you from inadvertently overriding a parent method you may not even have noticed was there,
thus changing the behavior of the code in unexpected ways.

We then obtain the almost-final variant of our `respond()` method:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_10.scala">Load into Editor</button>

    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val generation = paramMap("generation").toInt
            if( generation == 0 ) {
                if( paramMap("energy").toInt >= 100 && rnd.nextDouble() < 0.05 ) {
                    val heading = XY.random(rnd)
                    "Spawn(direction=" + heading + ",heading=" + heading + ")"
                } else ""
            } else {
                val headingStr = paramMap("heading")
                val heading = XY.fromString(headingStr)
                "Move(direction=" + heading + ")"
            }
        } else ""
    }

Note that in the construction of the `Spawn` and `Move` response strings, we are not explicitly
calling `XY.toString`. The compiler detects that, in order to assemble the concatenated values
into a string, it needs to convert our `direction` values from type `XY` into type `String`
and automatically inserts calls to `XY.toString` to accomplish that.


## Step 9: changing `fromString` to `apply`

The static method `XY.fromString()` is essentially a factory method that, instead of `Int` x and y
coordinates, takes a specially formatted `String` value as a parameter. So it makes sense to also
define an `apply()` function on `object XY`:

    object XY {
        def apply(s: String) : XY = {
            val xy = s.split(':').map(_.toInt) // e.g. "-1:1" => Array(-1,1)
            XY(xy(0), xy(1))
        }
    }

As before, because the function is overloaded, we need to declare the return value explicitly to
be of type `XY`.

After making this change, we can change the earlier code:

    val heading = XY.fromString(headingStr)

into this slightly more concise format:

    val heading = XY(headingStr)

Here is the final result:
<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_09_sample_11.scala">Load into Editor</button>



