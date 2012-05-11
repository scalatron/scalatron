---
layout: default
title: Bot #6 - Extracting the Command Parser
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_05.html' data-next='/tutorial/tutorial_20_bot_07.html' />

# Bot #6: Extracting the Command Parser

## Objective

In the preceding bot version we noted that the `parse()` method could really be a static
method. So let's turn it into one.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_06_sample_1.scala">Load into Editor</button>

    class ControlFunction {
        def respond(input: String) = {
            val (opcode, paramMap) = CommandParser(input)
            if(opcode=="React") {
                "Status(text=Energy:" + paramMap("energy") + ")"
            } else {
                ""
            }
        }
    }

    object CommandParser {
        def apply(command: String) = {
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

The code above uses the keyword `object` to define a container for our static parser function
(more on `object` below). The container gets the name `CommandParser` and it contains a static
method `apply()`, which is simply a renamed copy of our former `parse()` function. And the
method body of `respond()` invokes the static parsing function via a reference to the
`CommandParser` object. Here you should note the absence of an explicit mention of `apply()`
in the invocation and the way the parsed result is broken up into two values in parentheses -
both of which we'll explain below.



### What does `object` do?

The `object` keyword in Scala defines a container for static values and methods. Unlike
Java, where static fields and methods are mixed into the regular methods of a `class`, Scala
isolates static methods into such containers.

Here is an example of how you might use `object`:

    object BotConstants {
        val energyThreshold = 100
        val spawnDelay = 10
    }

and here is how code elsewhere could access these static values:

    class FooBar {
        def foo() {
            bar(BotConstants.energyThreshold)
            blop(BotConstants.spawnDelay)
        }
    }

An important concept in this context is that of a **companion object** for a `class`.
A companion object is a container for the static methods of a class that has the same
name as the class and that is defined in the same `.scala` file.



### Why is there no explicit call to `apply()`?

In the bot example code, we have the following line:

    val (opcode, paramMap) = CommandParser(input)

We now know that `CommandParser` refers to a container for static functions and values.
And evidently the code is intended to parse the input. But the parser function has the
name `apply()` and there is no mention of `apply()` here. What is going on?

In Scala, methods called `apply()` have special meaning. The most immediate practical
consequence is that for methods and functions whose name is `apply` you can omit the
method name and use just parentheses and the parameters. So the verbose variant of the
invocation above would be:

    val (opcode, paramMap) = CommandParser.apply(input)

We simply omitted the dot and the `apply` method name, because we do not need it.
You'll see various uses of this hand syntax later on, which will in many scenarios
make your code look much more readable and elegant (once you've figured out what
is going on, of course).



### What does `paramMap("energy")` do?

The complete line of code in our bot example is:

    "Status(text=Energy:" + paramMap("energy") + ")"

Overall this is a string concatenation in which the symbol `paramMap` refers to the
parameter map returned by the command parser, i.e. it is a reference to an instance of
a `Map[String,String]` whose keys are the parameter names and whose values are the
parameter values.

So we can already anticipate that this code will retrieve the value associated with
the key "energy". But why is this a valid method invocation, if we have an instance
but no dot or method?

The solution is the same as for the code that we used to parse the command string:
it is an invocation of a method of `Map` called `apply()`, in which Scala allows us
to omit the method name. The verbose variant would be this:

    "Status(text=Energy:" + paramMap.apply("energy") + ")"





### What does `(opcode, paramMap)` do?

So we know that `parse()` returns a tuple that Scala generates for us on the fly.
In the preceding example, we have used the following code to take apart the tuple
and obtain the individual element values:

    val parseResult = parse(input)
    val opcode = parseResult._1
    val paramMap = parseResult._2

Now, the code just reads

    val (opcode, paramMap) = CommandParser(input)

How does this work?

We already noted that the need for functions with multiple return values is
common and prompted the Scala designers to offer some syntactic sugar for
constructing and typing `Tuple` values. To complete the cycle, Scala also
offers syntactic sugar for taking the results apart again at the call site,
some of which is specific to `Tuple` and some is not.

The details are beyond what is required here, but the gist of the code above 
is this: Scala breaks the returned tuple up into its elements and assigns
the element values to the local values `opcode` and `paramMap`. 

If you want to read up on the concepts related to this extremely powerful
idea, look up "extractors" and "unapply" in the "Programming in Scala" book.


