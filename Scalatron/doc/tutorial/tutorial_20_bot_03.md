---
layout: default
title: Bot #3 - Checking the Opcode
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_02.html' data-next='/tutorial/tutorial_20_bot_04.html' />

# Bot #3: Checking the Opcode

## Objective

We now want to let the bot distinguish between the different commands that the game server
may pass as input parameters into the control function, such as "React" versus "Welcome".
To do that, we need to parse the input string provided by the server. Here is a simplified
input string (for details, check the Scalatron Protocol documentation):

    React(generation=0,time=0,view=__W_W_W__,energy=100)

We will modify the control function to now only respond with a command if the server sends
an input containing a `React()` opcode. To make the bot behavior a little more interesting,
we will have it return a `Move()` command, prompting the bot to run towards the right.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_03_sample_1.scala">Load into Editor</button>

The `ControlFunctionFactory` and `ControlFunction` boilerplate code remains unchanged from the
previous example; we replace only the `respond()` method:

    def respond(input: String) = {
        val tokens = input.split('(')   // split at '(', returns Array[String]
        if(tokens(0)=="React") {        // token(0): 0th element of array
            "Move(direction=1:0)"       // response if true
        } else {
            ""                          // response if false
        }
    }


## What is going on?

First, we need to extract the opcode from the command string. A simple way to achieve
this is to use one of the library methods that Scala offers for values of type `String`.
Namely, we use the `split()` method, which takes a value of type `Char` as a parameter,
to break the string into a collection of sub-strings at all occurrences of that character
value (here, occurrences of the opening parenthesis that immediately follows an opcode).

`String.split()` returns a result of type `Array[String]`. We extract the first array
element, which should now contain the string representing the opcode, and compare it
to the expected opcode. Unlike in Java, in Scala the comparison with `==` does what you
expect.

If the opcode is `React`, we return a `Move()` command. Otherwise we return an empty
string.


### What does `split()` do?

`String.split()` breaks a string into a collection of sub-strings at occurrences of a given
separator character. It is defined in the Scala library and has the following signature:

    def split(separator: Char) : Array[String]

So it takes a value of type `Char` as a parameter (the separator) and returns a result of
type `Array[String]` (the sub-strings).

Example: `"a(b)".split('(')` returns `Array("a", "b)")`



### What are some other methods available on `String`?

A good way to learn about the methods available at any point in your code is to use
your IDE's auto-complete method. This is highly recommended, since e.g. the Scala
plug-in for IntelliJ IDEA will also display methods available through implicit
conversions, something that is extremely convenient but initially somewhat hard to
fathom for people new to Scala.

Here are a few more methods available on String:

    val a = "Hello" + " " + "World"
    a.length            // = 11
    a.drop(6)           // = "World"
    a.dropRight(6)      // = "Hello"
    a.split(' ')        // = Array("Hello", "World")




### How does the `if` statement work?

Remember that in Scala, you should think of your code components as expressions. Here,
the if block is an expression whose result value is either the value of the true branch
or the false branch:

    if(condition) { true-block } else { false-block }

This is actually a bit similar to the '?:' operator, which is not
necessary in Scala.

Examples:

    val b = if(true) true else false
    val a = if(b) false else true
    val not : Boolean => Boolean = in => if(in) false else true



### What does `tokens(0)` do?

In Scala, you retrieve a particular element of an array by specifying its (zero-based)
index in simple parentheses. Since `tokens` is an `Array[String]` that was returned by
`String.split()`, the expression `tokens(0)` returns the first element (at index zero)
of that array.

Assignment to array elements works in the same manner. Here are some additional examples
of working with arrays:

    val nums = Array(1, 2, 3)
    val strings = Array("Hello", "World")
    val empty = Array.empty[Int]

    val s0 = strings(0)     // read
    nums(1) = 9             // write

