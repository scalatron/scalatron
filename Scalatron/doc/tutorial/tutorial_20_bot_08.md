---
layout: default
title: Bot #8 - Missile Launcher
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_07.html' data-next='/tutorial/tutorial_20_bot_09.html' />

# Bot #8: Missile Launcher

## Objective

Create a control function that will let our (master) bot launch missiles (mini-bots) at random
intervals in random directions and will guide the missile mini-bots along those directions.


## Source Code <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_20_bot_08_sample_1.scala">Load into Editor</button>

    class ControlFunction {
        val rnd = new Random()

        def respond(input: String): String = {
            val (opcode, paramMap) = CommandParser(input)
            if( opcode == "React" ) {
                val generation = paramMap("generation").toInt
                if( generation == 0 ) {
                    if( paramMap("energy").toInt >= 100 && rnd.nextDouble() < 0.05 ) {
                        val dx = rnd.nextInt(3)-1
                        val dy = rnd.nextInt(3)-1
                        val direction = dx + ":" + dy // e.g. "-1:1"
                        "Spawn(direction=" + direction + ",energy=100,heading=" + direction + ")"
                    } else ""
                } else {
                    val heading = paramMap("heading")
                    "Move(direction=" + heading + ")"
                }
            } else ""
        }
    }


## What is going on?

Much of the code is identical to the previous version: we define a field for a random
number generator and we parse the input command.

But then the code differentiates between the `generation` of the entity for which the control
function is invoked. To do that, the code extracts the value of the `generation` parameter from
the parameter map. Master bots have `generation` 0 (zero), while mini-bots have `generation`
1 (one) or higher. The game server provides this parameter to indicate to our control function
for which of the controlled entities it wants a response.

    val generation = paramMap("generation").toInt

The master bot will always see this value as 0 (zero), but mini-bots that we spawned will see
a 1 (one). So we can follow this with an `if` expression:

    if( generation == 0 ) {

In the true-block, we have the code for our master bot. It checks whether the bot has
enough energy to spawn a mini-bot by checking the "energy" parameter passed by the game
server:

    if( paramMap("energy").toInt >= 100 /* ... */ ) {

And we use the random number generator to express that we want to launch a new missile
each cycle we're invoked with a probability of 5%:

    if( /* ... */ rnd.nextDouble() < 0.05 ) {

We then assemble a random launch direction for the missile:

    val dx = rnd.nextInt(3)-1
    val dy = rnd.nextInt(3)-1

and use the X and Y direction values to construct a string direction:

    val direction = dx + ":" + dy // e.g. "-1:1"

We will use this string as we concatenate and return a command string that tells the game
server to spawn a new mini-bot at the given offset (relative to the master bot):

    "Spawn(direction=" + direction + ",energy=100,heading=" + direction + ")"

But in addition to specifying the `direction` and `energy` parameters that are defined for
the `Spawn` opcode in the server/plug-in protocol, we're passing a custom state parameter
called `heading`. In doing so we exploit the fact that the server lets us use the `Spawn`
to initialize the state property of the new mini-bot with arbitrary additional values before
it is brought into existence in the arena.

In the false-block, we have the code for the case where the control function is invoked for any
of the mini-bots we spawned. The mini-bot needs to know in which direction to move. That is
where the custom state property `heading` comes into play which we initialized with `Spawn`.
The mini-bot fetches this property's value and uses it as its movement direction:

    val heading = paramMap("heading")
    "Move(direction=" + heading + ")"

Since we already stored the `heading` in the format appropriate for x/y coordinate values
("x:y"), we can simply pass the string value of the property as the direction of the `Move`
command.



