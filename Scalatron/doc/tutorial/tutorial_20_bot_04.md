---
layout: default
title: Bot #4 - Expanded Input Parser
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_03.html' data-next='/tutorial/tutorial_20_bot_05.html' />

# Bot #4: Expanded Input Parser


## Objective

Instead of a counter, we want to display the bot's current energy level next to it on the
screen. To do that, we first have to extract the energy from the input string and then set
it as the bot's status string using the `Status()` command. The server informs the bot about
its energy level via the `energy` parameter of the input string. Here is again an example
input string:

    React(generation=0,view=__W_W_W__,energy=100)

Since we'll soon need to extract all of the other parameters as well, the most sensible
way to proceed is to parse the entire parameter list.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_04_sample_1.scala">Load into Editor</button>

    // omitted: ControlFunctionFactory

    class ControlFunction {
        def respond(input: String) = {
            val tokens = input.split('(')
            val opcode = tokens(0)
            if(opcode=="React") {
                val rest = tokens(1).dropRight(1)               // "key=value,key=value,key=value"
                val params = rest.split(',')                    // = Array("key=value", "key=value", ...)
                val strPairs = params.map(s => s.split('='))    // = Array( Array("key","value"), Array("key","value"), ..)
                val kvPairs = strPairs.map(a => (a(0),a(1)))    // = Array( ("key","value"), ("key","value"), ..)
                val paramMap = kvPairs.toMap                    // = Map( "key" -> "value", "key" -> "value", ..)

                val energy = paramMap("energy").toInt
                "Status(text=Energy:" + energy + ")"
            } else {
                ""
            }
        }
    }


## What is going on?

The first two line uses `split()` to break the input into two parts: the opcode and the
remaining line:

    val tokens = input.split('(')

These two parts are returned in an array, from which we extract the first
element (index zero) and store it into a local immutable value:

    val opcode = tokens(0)

We then test the just-extracted opcode against the "React" constant:

    if(opcode=="React") {

The second element of the `tokens` array contains the parameters of the command,
plus a trailing closing parenthesis (the opening parenthesis was swallowed by
`split()`). To obtain a string containing just the comma-separated parameters, we
need to get rid of this trailing character. We do this using the String
method `dropRight()`, a method available on many collections which takes the
number of elements to drop as a parameter:

    val rest = tokens(1).dropRight(1)               // "key=value,key=value,key=value"

The `rest` value now contains a string consisting just of comma-separated key/value
pairs, as shown in the comment on the line above. So we use `split()` again to break
it apart, this time at occurrences of the comma character:

    val params = rest.split(',')                    // = Array("key=value", "key=value", ...)

The result is again an array of `String` values, each of which has the format
"key=value". So now we want to break apart **each one** of these strings using
`split()` at the equals sign. We achieve this by using `map()` to process every
element of the array, obtaining a new collection containing the processed
elements:

    val strPairs = params.map(s => s.split('='))    // = Array( Array("key","value"), Array("key","value"), ..)

So `strPairs` now is an array of arrays of String, with overall type `Array[Array[String]]`.
For easy lookups later on, we'd really like to have a map from keys to values. Conveniently,
there is the `toMap()` method that converts a collection of key/value pairs into a map.
Unfortunately, we don't have a collection of pairs but a collection of arrays. So we
process `strPairs` again to obtain a collection of key/value pairs:

    val kvPairs = strPairs.map(a => (a(0),a(1)))    // = Array( ("key","value"), ("key","value"), ..)

which we then convert into a map:

    val paramMap = kvPairs.toMap                         // = Map( "key" -> "value", "key" -> "value", ..)

The value `paramMap` now contains a reference to an instance of the Scala `Map`
collection, with type `Map[String,String]`. Now it is easy to extract the
value associated with the key "energy" and to convert it from a string into
an integer value:

    val energy = paramMap("energy").toInt

And finally we assemble the command string that updates the bot's status message:

    "Status(text=Energy:" + energy + ")"



### A Word on Scala Collections

Scala has a very extensive and very well-designed library that contains a number
of useful collection types, including:

* List
* Set
* Map
* Queue

In many cases, there are two implementations for these collection types: a mutable
and an immutable one. In the mutable variants, operations modify the collection's
contents. In the immutable variants operations return a new, modified collection
without disturbing the original collection.

Scala defaults to immutable collections and for the bot code we will also exclusively
select these immutable collections. Immutability makes it much easier to reason
about our code and to parallelize operations, since we never need to worry about
synchronizing concurrent accesses. In certain performance-critical code sections it
may occasionally be advantageous to internally use a mutable collection with in-place
updating, but this is rarely the case and should be done only if profiling indicates
a need for it. As a rule of thumb, to get the benefits of functional programming we'll
embrace immutability as the default. The Scalatron server and the Scalatron bot
examples exclusively use immutable collections.

If you are interested in the ideas behind immutable collections and other data structures
and want to know more about their time and space characteristics, check out the book
"Purely Functional Data Structures" by Chris Okasaki.


### What does `dropRight()` do?

Given a collection of elements, we often want to do the same kinds of things with them:
extract elements, drop elements, split the collection, etc. The Scala library provides
implementations for many such operations. If an operation is not present in exactly the
form you need it, in many cases it is still possible to compose existing operations to
achieve your goal.

`dropRight()` is one example of such a collection method. It drops the last N elements
of a collection and returns the resulting new collection. In our bot example:

    val rest = tokens(1).dropRight(1)

we apply `dropRight()` to an instance of `String`. This works because there is an
implicit conversion from String into a collection type that presents its contents as
an indexed sequence (`IndexedSeq`) of characters.

In excruciating detail, our code

    val rest = tokens(1).dropRight(1)

does this:

* defines an immutable value 'rest'
* takes the element with index 1 in the array of Strings in 'tokens'
* this will be the right-hand side of the split, like "key=value,key=value)"
* invokes its method 'dropRight()'
* passing the parameter '1' for the number of elements to drop
* which returns a value of type 'String' without the closing parenthesis
* we can then split the key/value pairs at commas


### A few useful collection methods

Here are some other frequently used collection methods:

* head: returns the first element of a collection
* last: returns the last element of a collection
* tail: drops the first element of a collection and returns the resulting new collection
* drop(n): drops the first N elements of the collection and returns the resulting new collection

A few examples:

    List(1,2,3).drop(2)     // returns List(3)
    List(1,2,3).head        // returns 1
    List(1,2,3).tail        // returns List(2,3)
    List(1,2,3).last        // returns 3

As mentioned, many collection operations also apply to String, treating it as a
collection of `Char`:

    "abc".drop(1)           // returns "bc"
    "abc".dropRight(1)      // returns "ab"



### What does `(a(0),a(1))` do?

This expression constructs a pair (a tuple of arity two) containing the first and
second elements of an array value `a`. We'll talk about where the array comes from
in the next section, and for now focus only on the tuple creation.

Generally speaking, when writing methods, it is often necessary for a function to return
more than one value. Traditionally, for example in Java or C++, this requires the creation
of a parameter object, which involves the definition of a new named type (a class or struct)
and quite a bit of boilerplate code.

Scala already offers a built-in type `Tuple` with elements whose types are parameterized.
There are implementations for tuples for every arity up to 22. Here are some examples:

    val pair = Tuple2[Int,String](1, "A")
    val triple = Tuple3[Int,String,Char](1, "A", 'B')

So instead of defining a new class or struct for a parameter object, we can use a generic
Tuple to return more than one value from a function. Here is an example for a function
declared to return a pair of `String` values:

    def foo : Tuple2[String,String]     // function that returns a tuple

and here is an example implementation:

    def foo = new Tuple2[String,String]("Hello", "World")

for which the compiler can infer the types, so we can omit those:

    def foo = new Tuple2("Hello", "World")

and because `Tuple2` offers a static construction method (more on those later), we can
omit the `new` keyword:

    def foo = Tuple2("Hello", "World")

But Scala can do even better than that. Because using `Tuple` to return values is
so useful and hence used so frequently, Scala has special syntactic sugar to create
tuples and describe tuple types: you simple use parentheses around a comma-separated
list of values or types. Let's do that for our example:

    def foo = ("Hello", "World")

Here are some other examples for the use of tuples:

    val pair = (1, "A")                     // = Tuple2[Int,String]
    val triple = (1, 2, 3)                  // = Tuple3[Int,Int,Int]

Scala's syntactic sugar not just helps with creating tuples, it also extends to
declaring tuple types. In the original declaration of our example function `foo`,
we had:

    def foo : Tuple2[String,String]     // function that returns a tuple

Using the tuple syntax we can now write the exact same declaration like this:

    def foo : (String,String)           // function that returns a tuple

We can use this wherever a type would appear in Scala:

    def foo(pair: (Int, String))        // function accepting Tuple2[Int,String]
    def bar: (Int, String)              // function returning Tuple2[Int,String]
    val f = () => (Int, String)         // value: function returning Tuple2[Int,String]

How do you access the contents of a tuple? The tuple implementations in Scala
all contain fields with names like _1, _2, _3, etc. Here is an example:

    val a = ("A","B")._1                    // extracts the first field of the tuple
    val b = ("A","B")._2                    // extracts the second field of the tuple

Some more examples for the use of tuples:

    val tuple = ("A",2)                             // = Tuple2[String,Int] = (String,Int)
    val array = Array("A","B")                      // = Array[String]
    val tuple = (array(0),array(1))                 // = Tuple2[String,String] = (String,String)




### What does `map()` do?

The `map()` method appears in our example twice:

    val strPairs = params.map(s => s.split('='))    // = Array( Array("key","value"), Array("key","value"), ..)
    val kvPairs = strPairs.map(a => (a(0),a(1)))    // = Array( ("key","value"), ("key","value"), ..)

It is a method available on collections (like List, Set, Map) that, given a transformation
function, transforms every element of the collection into a new element and returns a new
collection containing the transformed elements.

It also works for `String` because `String` can act as a collection of `Char`. And,
even though `Array` in Scala is really just a Java Array which does not extend any of
the neat Scala collection traits, it also works for `Array` because there is an
implicit conversion that makes it available.

Let's pick apart the two lines that appear in our example in excruciating detail.

#### Example 1:

    val strPairs = params.map(s => s.split('='))

We start by using `val` to declare an immutable value that will be available in the scope of
our method. We tell the compiler that we will want to access that value by the name `strPairs`.
We then reference the collection we want to work on, here `params`, and use the dot-notation
familiar from Java or C++ to invoke the `map()` method on it.

The `map()` method takes one parameter, the transformation function.
This has to be a function that takes one parameter (an element of the collection) as its
input and that returns a new value. The type of the parameter must naturally be the type
of the element. The return value can and often will be of a different type. In the example,
the transformation function operates an a collection of `String` elements and therefore
will take an input parameter of type `String`. We invoke `split()` on each of these strings
and therefore convert each element into an `Array[String]`. Here, the return type of `split()`
is also the return type of our transformation function. It therefore has the following
type:

    String => Array[String]

i.e. it takes a `String` parameter and returns an `Array[String]` value.

The Scala collection library is very clever in how it implements `map()`. This has the
benefit that you will, whenever possible, get back a collection of the same type as the
one you operated on. Invoking `map()` on a `List` will result in a new `List`, invoking
it on a `Set` will result in a new `Set` and invoking it on a `Map` will result in a
new `Map`. This is not trivial. The downside is that the type signature of `map()`
looks scary as hell to people new to Scala. So we won't show it here. But it does what
you expect it to do and you can for now should use it without worrying about the
details. You will have absolutely not problem working out the details once you're more
familiar Scala.

So all that is left for us to do is to somehow tell the compiler that we want the
argument to `map()` to be an invocation of `split()` on every element of the collection.
That is what the code in the parentheses after `map` does:

    s => s.split('=')

Let's first re-write this code using a slightly more verbose but semantically identical
syntax, which we will then explain and show how to get to the simplified version. Here
is the verbose version:

    (s: String) => { s.split('=') }

This defines a function value by specifying, from left to right:

* the function's input parameter list: `(s: String)`
* a symbol that tells the compiler "this is a function": `=>`
* the body of the function: `{ s.split('=') }`

The input parameter list here consist of a single `String` parameter, to which we assign the
name `s`. Since the compiler knows the type of the collection on which we invoke `map()` and
it knows the signature of `map()`, it can infer the type of the input of our transform
function. It already knows that `s` will be a `String`. It therefore allows us to drop the
explicit mention of the type from our function definition. This gives us:

    (s) => { s.split('=') }

Since there is only a single parameter, we can also omit the parentheses. Like this:

    s => { s.split('=') }

Now on the right-hand side of the `=>` we have the function body. It invokes `split()`
with the argument `s`, which obviously contains whatever was passed as the input parameter
to the function; here, consecutively each element of the collection. Since the function
body consists only of a single expression, we can omit the curly braces. This gives us:

    s => s.split('=')

Which is exactly what we have in the example.

Scala allows us to express this even more concisely, however. We did not use this in the
example above to keep things simple to start with, but you can now try this out right away.
Essentially, it is very common for functions like the one we're passing to `map()` to
reference their parameters exactly once, for example to invoke a method on them. In instances
where this is the case, there is really no need to assign a name to the parameter - better
to keep it short. Scala allows us to make such an "anonymous" reference to a parameter by
using an underscore. So we can actually rewrite the example code like this:

    val strPairs = params.map(_.split('='))

When the `map()` method is executed at run time, it will iterate over the collection
elements of `params` and invoke `split('=')` on each one. This generates a new collection
containing elements of type `Array[String]`, which is then available through the value
`strPairs`. So when we start, `params` might contain the following value:

    Array("a=b", "c=d", "e=f")

Afterwards, `strPairs` would contain the following value:

    Array(Array("a", "b"), Array("c", "d"), Array("e", "f"))



#### Example 2:

    val kvPairs = strPairs.map(a => (a(0),a(1)))

The intent is to convert the array of arrays of strings into an array of pairs of strings,
so that we can then turn it into a map using `toMap()`. This is achieved by invoking the
`map()` method on `strPairs`, which has type `Array[Array[String]]`, and using a transform
function that turns each element of the collection (which will be an `Array[String]` that
was returned by `split()`, see above) into a pair of strings. Here is the transform
function in isolation:

    a => (a(0),a(1))

This is the concise equivalent of a more verbose version, like what we had in the
previous example:

    (a: Array[String]) => { (a(0),a(1)) }

Which would be even more verbose if we did not have the syntactic sugar for tuple creation
and type inference for the tuple's parameter types:

    (a: Array[String]) => { new Tuple2[String,String](a(0), a(1)) }

Just think for a moment about the amount of boilerplate code you'd have to write to
do this in whatever language you are using now, the time it takes to type all that
stuff in and the opportunity for introducing errors that generates.


### Wrapping up

The example uses a lot of named values to clarify the meaning of each computational
step. Like this:

    val rest = tokens(1).dropRight(1)               // "key=value,key=value,key=value"
    val params = rest.split(',')                    // = Array("key=value", "key=value", ...)
    val strPairs = params.map(s => s.split('='))    // = Array( Array("key","value"), Array("key","value"), ..)
    val kvPairs = strPairs.map(a => (a(0),a(1)))    // = Array( ("key","value"), ("key","value"), ..)
    val paramMap = kvPairs.toMap                    // = Map( "key" -> "value", "key" -> "value", ..)

We could of course draw all this together into a single invocation chain. The
result would look like this:

    val paramMap =
        tokens(1)
        .dropRight(1)
        .split(',')
        .map(_.split('='))
        .map(a => (a(0),a(1)))
        .toMap

and the entire control function would look like this: <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_04_sample_2.scala">Load into Editor</button>

    def respond(input: String) = {
        val tokens = input.split('(')
        val opcode = tokens(0)
        if(opcode=="React") {
            val paramMap =
                tokens(1).dropRight(1).split(',').map(_.split('='))
                .map(a => (a(0), a(1))).toMap
            val energy = paramMap("energy").toInt
            "Status(text=Energy:" + energy + ")"
        } else {
            ""
        }
    }



### More on Scala Maps

Like for most collection types, there are mutable and immutable versions of `Map`.
Here are a few more examples of how to create a map and work with its contents:

        val map = Map("A" -> 1, "B" -> 2, "C" -> 3)     // = Map[String,Int]

        val tuples = Array( ("A",1), ("B",2), ("C",3) ) // = Array[(String,Int)]
        val map = tuples.toMap                          // = Map[String,Int]

        val value = map(key)

The appendix of this tutorial contains a cheat sheet with useful methods of `Map`

