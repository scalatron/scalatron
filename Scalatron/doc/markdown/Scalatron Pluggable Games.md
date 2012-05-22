---
layout: default
title: Scalatron Pluggable Games
subtitle: Developer Documentation
---

# About Scalatron

Scalatron is an educational resource for groups of programmers that want to learn more about
the Scala programming language or want to hone their Scala programming skills. It is based on
Scalatron BotWar, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other.

The documentation, tutorial and source code are intended as a community resource and are
in the public domain. Feel free to use, copy, and improve them!



# About Pluggable Games

The original version of Scalatron includes only a single game, called *BotWar*. This game is supported
by a custom tutorial, example bots and pretty extensive documentation, which probably makes it a good
entry point for new developers interested in Scala.

However, having only a single game imposes some limitations. Once you've written a bot for that one
game, and tuned it to your best ability, there is really no follow-on challenge within the context of
Scalatron. And once the source code for good bots for that game is available on-line, as is the case
now, it becomes more difficult to organize programming tournaments where everyone gets a fair start.
It's a bit like a game console with only one cartridge.

The obvious solution: more games! Not only can players and workshop organizers then choose from a
variety of games, but writing a game that others can compete in now becomes an interesting project,
a kind of meta-programming-game.

Obviously, any experienced programmer can write a game like BotWar, and probably a much better one.
But as the implementation of Scalatron demonstrates, there is lots of fiddly work involved in creating
the infrastructure that is needed for a reasonably robust multi-player programming game. And, at least
for most people, polishing and testing those parts is much less fun than creating the logic and graphics
of an actual game.

So the obvious next step is to turn Scalatron into a platform for programming games by making the games
themselves plug-ins for the Scalatron server. Scalatron provides the basic services, such as a browser-based
editor, background compilation, a RESTful web API, bot plug-in management and a command line interface.
You can then focus on creating a game that is challenging and fun.



# Architecture Primer


## Scalatron Architecture

There is a separate document that provides an overview of the
[Scalatron Architecture](https://github.com/scalatron/scalatron/blob/master/Scalatron/doc/markdown/Scalatron%20Architecture.md),
so we won't repeat that here. But there are some aspects that are of specific interest for folks interested
in developing game plug-ins.


## Startup Sequence

When starting up, the Scalatron server will first launch its internal support services (web server, compilation
service, etc.) and then load a game and ask it to start the tournament loop.

The game to be loaded can be specified on the command line with the parameter `-game {name}`, which defaults
to `BotWar`. The server will look for a `.jar` file with this name in the `/bin` directory below the main
installation directory. For `BotWar`, for example, it would look for a file called `Scalatron/bin/BotWar.jar`.

Within this `.jar` file, Scalatron then attempts to locate a class with the fully qualified class name
`scalatron.GameFactory`. When such a class is found, Scalatron next looks for a method on that class called
`create`, taking no parameters and returning an instance of a class implementing the trait `scalatron.core.Game`.

Using this factory, Scalatron then creates an instance of a class implementing the trait `scalatron.core.Game` -
the entry point of your game implementation.


## Headless Or Visual?

Next, Scalatron checks the command line for an argument `-headless yes|no` to see whether the user wants visual
output (`-headless no`) or whether it should run in *headless* mode (`-headless yes`) with no main window.
Depending on what the user selected, Scalatron then passes control to your game plug-in by invoking one of the
following methods on your `Game` implementation:

  def runVisually(rounds: Int, scalatron: scalatron.core.ScalatronInward)
  def runHeadless(rounds: Int, scalatron: scalatron.core.ScalatronInward)

The parameters are:

* `rounds`, which specifies the number of tournament rounds (games) you should run before returning, and
* `scalatron', which is a reference to a `trait ScalatronInward`, the API Scalatron exposes toward
   game plug-ins (as opposed to `ScalatronOutward`, which is what it exposes to external users, such as the main
   function and the web server).


## Implementation Details

Once control passes to `runVisually()` or `runHeadless()`, it's up to you what happens next. For details,
you can refer either to the simple example implementation in [ScalatronDemoGame](https://github.com/scalatron/scalatron-demo-game)
or to the full and rather more complex implementation of [BotWar](https://github.com/scalatron/scalatron/tree/master/BotWar/src/scalatron).

Roughly speaking, your plug-in should be doing the following things:

* run an outer loop, iterating over game rounds
* at the start of each round, ask Scalatron for a collection of control functions representing the bots
* run an inner loop, iterating over the simulations steps within a game; within each step:
* update the graphical display, drawing the entities in the game and each player's score
* compute what your entities can see and ask their control functions for appropriate responses
* decode the responses (presumably commands) and update the game state and scores as appropriate
* whenever a round ends, tell Scalatron about the results


## Loose Ends

There are a few more aspects you could pay attention to, even though they are not required for a minimal
implementation (and may still change as the whole concept of pluggable games gets refined). These include:

* the method `Game.cmdArgList` is intended to enumerate the command line arguments that your game implementation
  understands and that a user can provide to configure your game. The BotWar game, for example uses settings like
  `x`, `y` and `walls` to configure the size of the arena and how many wall elements should be placed.

* the method `Game.startHeadless()` is invoked when a user starts a private, "sandboxed" game in the browser.
  Your plug-in is supposed to return the starting state of a game round. However, this is currently optimized
  only for BotWar and otherwise untested and unsupported, so you may or may not be able to do something interesting here.

* the method `GameState.entitiesOfPlayer(name: String)` is invoked when the browser-based "sandbox" debugger
  requests information about the entities controlled by a particular player. Again, this is currently optimized
  only for BotWar and otherwise untested and unsupported, so you may or may not be able to do something interesting here.




# How To Write A Game Plug-In For Scalatron

## Step 1: Pick A Name

Pick a name for your game. Then derive a standardized name from it that contains no spaces or other characters
that would be illegal for a Scala `class` or `package` identifier. Something like `BotWar` or `PandaWood`.
For this example, we'll use `MyGame`.


## Step 2: Create The Project

Create a directory structure for your project. The easiest way to do this is probably by copying and renaming
the [ScalatronDemoGame template on Github](https://github.com/scalatron/scalatron-demo-game).

The layout can be extremely simple:

    /MyGame
        /src
            /scalatron
                Game.scala

Your game plug-in will rely on the following libraries, which you will need to add as dependencies
to your SBT build file or to your IDE-specific project file:

    ScalatronCore.jar
    akka-actor-2.0.jar (Akka 2.0)
    scala-library-jar (Scala 2.9.1)

You can find the first library, `ScalatronCore.jar`, in the Scalatron installation directory of a Scalatron
distribution of version 1.1.0.0 or later.

You will then need to configure your project to generate a Java Archive (.jar) artifact with the appropriate
name, in our case `MyGame.jar`. You can build this wherever you want, but to activate it, it will eventually
have to end up in the Scalatron installation's `/bin` directory.


## Step 3: Implement The Game Logic

A minimal game plug-in will consist of a small number of required classes, plus whatever other classes
you may create to implement the game logic.

### Implement The `Game` Trait

Implement a class `scalatron.Game` (or `scalatron.myGame.Game` if you want a custom package - it does not matter)
that implements the `scalatron.core.Game` trait, like so:

    package scalatron

    case object Game extends scalatron.core.Game {
        ...
    }


### Implement A `GameFactory` Class

Implement a class `scalatron.GameFactory`, like so:

    package scalatron

    class GameFactory { def create() = scalatron.myGame.Game }


### Implement Additional Classes

Flesh out the functionality of your `Game` implementation, starting with the method `runVisually()`.
Please check out the example code of the `ScalatronDemoGame` and the outline of the overally architecture above
for details.



## Step 4: Run Your Game

Run the Scalatron server, telling it to load your game plug-in:

    cd Scalatron/bin
    java -jar Scalatron.jar -game ScalatronDemoGame

Note that you need to use a sufficiently recent version of the server that provides support for pluggable
games. This feature is supported beginning with version 1.1.0.0, an experiemental release.



## Step 5: Create A Bot Plug-In

Obviously you'll want one or more bots in your game. You will need to compile these bots as outlined in the
[Scalatron Player Setup](https://github.com/scalatron/scalatron/blob/master/Scalatron/doc/markdown/Scalatron%20Player%20Setup.md)
guide.

The easiest way to do this is via the browser-based editor that is part of the Scalatron IDE provided by the
Scalatron server. Follow these steps:

* Launch the Scalatron server app, as described above
* This should automatically bring up a browser window pointing at the correct address
* Create one or more user accounts that will be associated with your bots, say `PlayerA` and `PlayerB`
* Log in as each of these players in turn
* Create a source code file that contains the required `ControlFunctionFactory` implementation (see the Player Setup guide)
* In the editor toolbar, click **Publish into Tournament**
* This will upload the source code, build it and publish the bot into the tournament

The next time your game plug-in starts a game round and fetches a fresh collection of `EntityController`
instances from Scalatron, your bots should be part of them and show up in your game.



## Step 6: Invite Some Friends And Run A Tournament

Obviously, some minimal preparatory work is required on your part:

* write some documentation for the rules of your game (see the [Scalatron Game Rules for BotWar](https://github.com/scalatron/scalatron/blob/master/Scalatron/doc/markdown/Scalatron%20Game%20Rules.md) for an example)
* write some documentation for the game/bot protocol of your game (see the [Scalatron Protocol for BotWar](https://github.com/scalatron/scalatron/blob/master/Scalatron/doc/markdown/Scalatron%20Protocol.md) for an example)
* write a few simple bots as examples and for testing purposes
* do some testing :-)



# Missing Pieces

As of this writing, pluggable games are an experimental feature available in special releases of
Scalatron with a version number of 1.1.0.0 or higher. Lots of polishing is still missing for full
support of this feature, but that should not keep you from playing around with it.

Here is what seems to be missing:

* the browser-based UI will show the standard tutorial, which is focused on the BotWar game.
  In a future version, you will be able to provide a custom tutorial from within your game plug-in,
  which will be displayed by the browser UI using standardized entry points (starting page and index page).
* the browser-based UI, when **Run in Sandbox** is clicked, will show the BotWar debugger, which will
  very likely not work at all. In a future version, you should be able to replace this panel with a custom
  debugger / state monitor loaded from your game plug-in.
* the samples provided in the `Scalatron/samples/` directory are the sample bots for BotWar.
  Each game should have its own sample bots, obviously. Maybe the best way is to get rid of the samples
  in the installation altogether and to provide them online (maybe as *gists*) as well as through the tutorial
  panel.
* the secure mode of Scalatron is still a work in progress, so some utility methods may be provided here
  to make it easier to process your bots using Akka Futures in a way that times out and disables unruly or
  broken bots.
* probably lots of other details, including installation details, separate bot directories for each game, etc.

So if you create a game plug-in, please keep in mind that these things will probably still be fiddled with.

