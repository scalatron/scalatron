---
layout: default
title: Bot #7 - Brownian Motion
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_06.html' data-next='/tutorial/tutorial_20_bot_08.html' />

# Bot #7: Brownian Motion

## Objective

Create a bot that will meander around the arena using random motions.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_07_sample_1.scala">Load into Editor</button>

We will from now on omit the code for the `CommandParser` object, since we will never
again change it. Just leave it in your source file, just like we did with the
`ControlFunctionFactory` class.

    import util.Random

    class ControlFunction {
        val rnd = new Random()
        def respond(input: String): String = {
            val (opcode, paramMap) = CommandParser(input)
            if( opcode == "React" ) {
                val dx = rnd.nextInt(3)-1
                val dy = rnd.nextInt(3)-1
                "Move(direction=" + dx + ":" + dy + ")"
            } else {
                ""
            }
        }
    }


## What is going on?

In the first line, we add an `import` statement that will pull in the definition of the
`scala.util.Random` class, a pseudo-random number generator (actually a Scala wrapper for
`java.util.Random`):

    import util.Random

This makes the symbol `Random` available to the code in our source file.

We then add a new field called `rnd` to our `ControlFunction` class which will hold a reference to an
instance of such a pseudo-random number generator, which we simply create with `new`:

    val rnd = new Random()

Further down, we use the generator to produce random X and Y values for the movement
direction of our bot:

    val dx = rnd.nextInt(3)-1
    val dy = rnd.nextInt(3)-1

This code uses the member function `nextInt(n: Int)` of `Random`, which returns a 
value in the range `0` to `(n-1)`. 

We then combine the generated coordinate values with the Scalatron command opcode 
`Move` (see the Scalatron Protocol documentation) to assemble the bot's response:

    "Move(direction=" + dx + ":" + dy + ")"

The result is a bot that, every time it is asked by the game server what it wants to do,
requests the be moved once cell in some random direction.


