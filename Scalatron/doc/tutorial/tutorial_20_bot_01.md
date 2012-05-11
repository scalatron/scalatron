---
layout: default
title: Bot #1 - Hello World
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_00_30_protocol.html' data-next='/tutorial/tutorial_20_bot_02.html' />

# Bot #1: Hello World

## Objective

Create a minimal bot that compiles and appears in the game.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_01_sample_1.scala">Load into Editor</button>

    class ControlFunctionFactory {
        def create = new ControlFunction().respond _
    }

    class ControlFunction {
        def respond(input: String) = "Status(text=Hello World)"
    }




## Building Your Bot

To see your bot in action, follow these steps:

* click on the <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_01_sample_1.scala">Load into Editor</button> button
   next to the code you'd like to use, for example above the source code snippet just above. This will load the code into the editor at right.
* in the toolbar above the editor, click the **Publish into Tournament** button.
   This will upload the source code from the editor to the Scalatron server, compile it,
   compress it into a bot plug-in and publish that bot plug-in into the tournament loop.
   When the next tournament round starts, your bot will be picked up and appear on the screen.

Later, you will generally want to test and debug your bot before you publish it into the
tournament. Here's how would you do that:

* in the toolbar above the editor, click the **Run in Sandbox** button.
   This will upload the source code from the editor to the Scalatron server, compile it,
   compress it into a bot plug-in and launch a private, sandboxed game on the server that
   contains only your bot.
* if the bot compiled without errors, a panel will slide out on the right-hand side of the
  screen that lets you **Run** your bot inside the sandbox, **Step** through the simulation
  one-by one, **Step 10** iterations at once or **Restart** the simulation.
* the bot view shows that part of the arena which your bot can see at that point. Your bot is
  the colored dot in the middle. Below the bot view, you can see the **Input** that your bot
  received, the **Output** commands that your bot generated and any **Log** output your bot
  generated via the `Log()` command.

Now let's look at the code in detail...



## What is going on?

The code defines two classes, `ControlFunctionFactory` and `ControlFunction`. In each class, it defines one
method. The method `ControlFunctionFactory.create()` constructs a new instance of the class `ControlFunction`
and returns a reference to its `respond()` method. This is the control function that the game
server will invoke to interact with the bot.

The method `ControlFunction.respond()` receives a `String` parameter (which it ignores)
and returns a constant string that contains a command for the game server, in this case the
command Status() which asks the server to set the status text of the bot to "Hello World".


## What does `class` do?

The `class` keyword begins the definition of a new class.

Examples in our bot:

    class ControlFunctionFactory { ... }
    class ControlFunction { ... }


## What does `def` do?

The `def` keyword defines a function or method.

Examples in our bot:

* `def create = new ControlFunction().respond _`
  * defines a method create() in the class `ControlFunctionFactory`
  * that takes no parameters
  * that returns a function of type `String => String`
  * the return type is inferred by the compiler from the method body
  * the body uses new to create an instance of class `ControlFunction`
  * it then returns a reference to the `respond()` method of class `ControlFunction` whose execution context is the instance we just created
* `def respond(input: String) = "Status(text=Hello World)"`
  * defines a method `respond()` in the class `ControlFunction`
  * that takes one parameter of type String called input
  * that returns a String
  * the return type is inferred by the compiler from the method body
  * the body consists of the String constant `"Status(text=Hello World)"`

Other examples:

    def foo: Int = 1                    // function without side-effects
    def foo = 1                         // Scala infers return type

    def bar(s: String): String = "Hello"
    def bar(s: String) = "Hello"        // Scala infers return type

    def bar(): Unit = { println(..)     // function with side-effects
    def bar() { ... }                   // Scala infers return type



## How are types specified?

In Scala, names precede types:

    def foo(input: String) : String = ""
    val bar: Int = 1
    val l: List[Int] = List(1, 2, 3)

Types can be omitted if they can be inferred by the compiler:

    def foo(input: String) = ""
    val bar = 1
    val l = List(1, 2, 3)



## What types are available in Scala?

Scala has all the basic types of Java:

* `String`
* `Int`
* `Double`
* `Char`
* `Array[T]`

Scala also has functions as values and has the appropriate types.
Example: a function that takes a string and returns a string:

    val foo: String => String = input => input + "."

If we want to omit the type on the left-hand side, we need to tell the compiler
about it on the right-hand side:

    val foo = (input: String) => input + "."



## Why is there no `return` statement?

In Scala, and in functional programming in general, you should think of methods and most
code elements as expressions that yield the result of the expression as their value.
Instead of writing, for example, code like:

    int n = 0
    if(x.isEmpty) n=1 else n=2

we can write

    val n = if(x.isEmpty) 1 else 2

Likewise, we can view entire methods as expressions that yield a value. Instead of writing

    def respond(input: String) = { return "" }

we can write

    def respond(input: String) = ""

Scala does have a `return` statement, but using it is frowned upon as poor functional
programming practice and occurrences of `return` are considered by many as a "code smell".
If you do want to use return (for example to break out of a loop), note that you must
explicitly specify the return type of the method from which you want to return.



## What does the underscore after `respond` mean?

From the `create()` method, we want to return a function value, namely the method `respond()`
of our newly created `ControlFunction` instance. The underscore, by appearing instead of the parameter
list of `respond()`, tells the compiler that we do not wish to invoke the function but to
turn it into a value. If you are not familiar with functional programming, this may be a
bit confusing but don't be put off by it - you do not yet need to understand the details
of this right now and it'll appear quite trivial later on.


