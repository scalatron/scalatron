---
layout: default
title: Bot #2 - Counting Cycles
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_01.html' data-next='/tutorial/tutorial_20_bot_03.html' />

# Bot #2: Counting Cycles

## Objective

Make the bot display on the screen the number of times its control function was invoked.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_02_sample_1.scala">Load into Editor</button>

    // ControlFunctionFactory definition is done as it stands
    // from now on, we'll omit it

    class ControlFunction {
        var n = 0
        def respond(input: String) = {
            val output = "Status(text=" + n + ")"   // temp value
            n += 1                                  // increment counter
            output                                  // yield
        }
    }


## What is going on?

We're from now on omitting the definition of the ControlFunctionFactory class,
since it will never change. Just leave it at the top of your source file for now.

In the code, we add a mutable field `n` to the definition of our `ControlFunction` class. From the
assignment of the zero value, the compiler infers that this field will have type `Int`.

In our `respond()` method, instead of returning a constant string as the status message,
we now construct a new string every time by concatenating multiple sub-strings and storing
the result in a temporary, immutable value called `output`. One of these sub-strings is a
String rendering of our counter field. The compiler conveniently converts the Int into a
String for us.

We then increment the counter by one and return the string we constructed earlier.
We indicate that we want to return the constructed string by simply making a reference
to the `output` value the last expression in the method.

This "yielding" behavior of functional languages is also the reason we create the temporary
value in the first place: if `n += 1` was the last expression in our method, that would also
be the return value of the method.


### What does `var` do?

The `var` keyword defines a new mutable value, which can be a local variable in a method
or a mutable field of a class. Note that mutability is generally frowned upon by functional
programmers since it makes it more difficult to reason about the state and behavior of
programs.


### What does `val` do?

The `val` keyword defines a new immutable value, which can be a local constant in a method
or an immutable field of a class. Because it is immutable, its value can never change.


### Where do I need curly braces?

Generally speaking you need curly braces to define the body of a class and to group
multiple statements into a block. That's why this method definition does not need curly braces:

    def respond(input: String) = "Status(text=" + n + ")"

but this one does:

    def respond(input: String) = {
        val output = "Status(text=" + n + ")"
        n += 1
        output
    }




### Where do I need a semicolon?

Unlike in Java, you do not need a semicolon at the end of every statement. For the moment,
the only reason you might want to use a semicolon is to separate multiple expressions on
the same line, like this:

    def respond(input: String) = { n += 1; "Status(text=Hello)" }




### Why do we store the output in a temporary constant?

Scala yields the value of the last expression in a method as the result value of that
method. To see what this means, consider the following variants of the code above:

**Step 1**: mutable field, but no incrementation <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_02_sample_2.scala">Load into Editor</button>

    class ControlFunction {
        var n = 0           // declare mutable field
        def respond(input: String) = "Status(text=" + n + ")"
    }

This code obviously always returns the same string.


**Step 2**: incrementation before concatenation <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_02_sample_3.scala">Load into Editor</button>

    class ControlFunction {
        var n = 0
        def respond(input: String) = {  // curly braces for scope
            n += 1                      // increment counter
            "Status(text=" + n + ")"
        }
    }

This code returns a string containing an incrementing counter, but the counter does not
start at zero. Because it is incremented before the string is constructed, the first
value returned is one.

Try it out by clicking **Load into Editor** above, then clicking **Run in Sandbox** above
the editor and then **Run** in the sandbox viewer that appears on the right. In the **Input** and
**Output** panels you will see how the counter in the status text increments.


**Step 3**: <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_02_sample_4.scala">Load into Editor</button>

    class ControlFunction {
        var n = 0
        def respond(input: String) : String = {
            "Status(text=" + n + ")"
            n += 1                          // BUG: yields Int (the updated n), not string
        }
    }

This now constructs the correct text, but instead of returning the text it returns... nothing.
The last statement of the `respond()` method is `n += 1`, which is a shorthand for `n = n + 1`,
which does not yield a value. So the compiler (correctly) infers the type of the function to
be `String => Unit`, where `Unit` is the Scala equivalent of Java's `void`.

If we do not explicitly declare the return type of the function to be `String`, as shown above,
this means that the compiler will generate `respond()` with a `Unit` return type and will
consequently also generate the factory method as `() => (String => Unit)`. Because the compiler
has no idea what we want, it will not report an error. But when we try to publish the bot into
the tournament, the Scalatron server will reject it because it can't find a factory function
with the expected signature `() => (String => String)`.

If we explicitly declare the return type of the method as `String`, however, as we do in the
code above, then the compiler can notice the discrepancy and complain about it.

Try it out by clicking **Load into Editor** above, then clicking **Build** above the editor.
This will display an error *type mismatch; found: Unit required: String* in the error console
below, as expected.


**Step 4**: <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_02_sample_5.scala">Load into Editor</button>

    class ControlFunction {
        var n = 0
        def respond(input: String) : String = {
            val output = "Status(text=" + n + ")"   // temp value
            n += 1                                  // now increments after use
            output                                  // yield
        }
    }

This creates a temporary constant value of type `String`, then increments the
value, and finally references the constant value again to make it the return
value of the function. Now everything works as expected.



