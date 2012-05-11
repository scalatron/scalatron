---
layout: default
title: Bot #5 - Creating a Command Parser Function
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_04.html' data-next='/tutorial/tutorial_20_bot_06.html' />

# Bot #5: Creating a Command Parser Function

## Objective

Having the somewhat complicated command parsing code in the middle of our control
function will be distracting. So we will pull it out into a separate function,
`parse()`. While we're at it, we'll also add some validation code.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_05_sample_1.scala">Load into Editor</button>

    class ControlFunction {
        def respond(input: String) = {
            val parseResult = parse(input)
            val opcode = parseResult._1
            val paramMap = parseResult._2
            if(opcode=="React") {
                "Status(text=Energy:" + paramMap("energy") + ")"
            } else {
                ""
            }
        }

        def parse(command: String) = {
            def splitParam(param: String) = {
                val segments = param.split('=')
                if( segments.length != 2 )
                    throw new IllegalStateException("invalid key/value pair: " + param)
                (segments(0),segments(1))
            }

            val segments = command.split('(')
            if( segments.length != 2 )
                throw new IllegalStateException("invalid command: " + command)

            val params = segments(1).dropRight(1).split(',')
            val keyValuePairs = params.map( splitParam ).toMap
            (segments(0), keyValuePairs)
        }
    }


## What is going on?

The code in the `respond()` method should by now be obvious. The primary change is that
instead of parsing the input string locally, we invoke a newly defined function `parse()`.

The parse function is defined within the body of our `ControlFunction` class, which makes it a
method with access to the `ControlFunction` instance (via `this`) - something we do not really
need here. It could just as well be a static function. In the next example we will
make this change, but for now let's look at the method as it stands.



### What is the Return Type of `parse()`?

We might note that in the first line of the method definition

    def parse(command: String) = {

no return type is specified. The `=` indicates to the compiler that a value will be
returned, but we leave it up to the compiler to figure out what the return type is.
After looking at the method definition (which we'll also do in just a minute) the
compiler will infer it to be:

    (String, Map[String, String])

i.e. a tuple (pair) containing a `String` as well as a `Map` from `String` to `String`.
In the first element of the tuple `parse()` will return the opcode of the command.
In the second element it will return the parameter map. Its keys are the parameter
names and its values are the parameter values.

Here is an example of what the `parse()` method expects as its input:

    "React(generation=0,energy=100)"

and what it will return:

    ("React", Map( "generation" -> "0", "energy" -> "100") )







### Why is there a `def` in the body of `parse()`?

To structure our parsing code, we use a local helper function `splitParam()` to
break apart the individual key/value pairs. Since this helper function is not
used or useful anywhere else in our code, we can keep it private to the method
that needs it. Scala allows us to nest functions as deeply as we want, so we
simply defined `splitParam()` within `parse()`, keeping the symbol local to
the function.



### Can my Bot just throw an exception?

Yes. The Scalatron game server will catch and process all exceptions thrown
within the bots. It also won't send you any invalid commands, but knowing how
to validate your inputs and how to throw an exception is useful.

When an exception is thrown, just as in Java, control escalates up the call
stack to the nearest exception handler. This is an instance where no value
is returned from a function.

Actually, just for reference, here is how you catch an exception in Scala:

    try {
        ... code that might throw an exception...
    } catch {
        case e: Exception => ... code that handles the exception ...
    }

The code within the `catch` block uses pattern matching, a powerful feature
that we will look at later on.

Note that the entire code block above is also an expression. If no exception
is thrown, it yields the value of the code within the `try` block. If an
exception is thrown, it yields the value of the exception handler.


### What does `map(splitParam)` do?

It is another example of syntactic sugar offered by Scala to eliminate
boilerplate and make your code more concise. The full line in the example
reads:

    val keyValuePairs = params.map( splitParam ).toMap

We already looked at how `map()` works: it expects a transformation function
as its only parameter. Here, the transformation function is obviously `splitParams`,
the local function we defined to take apart the key/value pairs. But the syntax
we use here is new: it contains neither a named parameter nor an underscore as an
anonymous placeholder. Instead, it exploits the fact that - in the frequently
occurring case of a function whose body is simply another function that takes the
input parameter of the outer function as its parameter - we can omit the parameter
altogether. The verbose variant of the line above would be:

    val keyValuePairs = params.map(s => splitParam(s)).toMap
