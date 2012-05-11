---
layout: default
title: Introduction To Scalatron
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_00_01_intro_to_scala.html' data-next='/tutorial/tutorial_00_20_game_rules.html' />

# Introduction To Scalatron

## Overview

The behavior of each bot is determined by an associated computer program fragment that
implements a control function mapping inputs (what the bot sees) to outputs (how the bot
responds).

This tutorial explains (very briefly) what Scala is and (in slightly more detail)
how to write a bot program in Scala. It is based on the following premises:

* you already know how to program, probably in Java or C++
* the quickest way for a programmer to understand almost anything is to look at sample code
  and to play around with working programs, modifying and incrementally improving them.

So this tutorial simply presents and analyzes the code to increasingly sophisticated bots,
which you can immediately try out and run in the game server. With each version, additional
Scala syntax is explained and the most important language constructs introduced. The idea is
to let you get from zero to a running bot that you can play with very quickly, and to let you
zip through the tutorial at your own pace, picking up useful tools as you go.

Have fun!






# How to get your bot into the game

Throughout the tutorial, you will create increasingly complex bot versions in Scala.
To see your bot appear in the game, you will need to compile and publish it.
There are two approaches to doing this, which in the Scalatron documentation are referred to
as the "serious" and "casual" paths, respectively.


## The "Casual" Path

The "casual" path is intended for less experienced programmers or for shorter bot coding sessions
(2-3 hours). On this path, players write, build, debug and publish their bots in a web browser.
The browser-based development environment is provided by an embedded web server which is hosted
inside the game server and requires no setup by the user.

If this is the path you want to follow, here is how to do it:

1. open a web browser and point it to the game server's address, which you can get from the
   workshop organizer. It will generally be something like `http://scalatron:8080`
2. log into the account associated with your name. If there is no account for you displayed
   on the log-in screen, ask the workshop organizer to create one for you.
3. enter your code in the editor that appears. You can also copy and paste code from the tutorial
   and example bot sources.
4. click the *Build* button. This will upload your code to the game server, compile it there,
   and display a list of errors (if there were any). Do this until your code compiles.
5. click the *Build and Run in Sandbox* button. This will upload and compile your code and then
   start a private, "sandboxed" game for your bot on the server. You can single-step through the
   simulation and observe the view and state of your bot. Tune your bot until you are happy with it.
5. click the *Build and Publish into Tournament* button. This will upload and compile your code and
   then publish it into the tournament loop, where it will be picked up automatically when the
   next game rounds starts.



## The "Serious" Path

The "serious" path is intended for experienced programmers planning for a longer bot coding
session (5-7 hours). On this path, bots are built locally by each player, using an IDE or command
line tools. The bots are then published into the tournament by copying them into the plug-in
directory on the central computer from which the game server loads them at the start of each
round.

If this is the path you want to follow, here is how to do it:


### Compiling

To compile your bot, you need to feed your Scala source file through the Scala compiler.
How you do this depends on the build environment you are using. If you are using the
IntelliJ IDEA setup described in the document *Player Setup*, all you will need
to do is select **Build > Make Project** from the main menu. This should perform the
following steps:

1. incrementally compile your source file into `.class` files using the *Fast Scala Compiler (FSC)*
2. package the resulting `.class` files into a Java Archive `.jar` artifact file
3. copy the `.jar` file into your bot directory on the game server

Once your plug-in `.jar` file was copied to the game server, it should be picked up as soon as
the next game round starts. You can see how far the current round has progressed and estimate
how long you'll have to wait in the bottom left corner of the screen, where it will say something
like:

    round 42: 240 steps (of 1000), 4.0s, 20ms/step


### Publishing

The publishing process basically consist of copying the `.jar` plug-in file you built
into your plug-in directory on the server.

This directory will be a sub-directory with your name below a network-shared directory
available to all users, which the workshop organizer should have made available to everyone.
Example:

* the organizer published a network-shared directory that is visible on your computer as
  `/Volumes/Scalatron/bots/`
* within this directory, if your name is Tina, you will at some point create a sub-directory
  called `Tina`
* into this directory you will publish (i.e., copy) your `.jar` file, which must be
  called `ScalatronBot.jar`
* the complete path where your plug-in resides is therefore
  `/Volumes/Scalatron/bots/Tina/ScalatronBot.jar`

For more details, see the *Scalatron Server Setup* guide and the *Scalatron Protocol*
documentation.



### Updating a bot plug-in

The details of how you publish a newly built bot plug-in depend on how the system running the
game server is configured.

In some configurations, new plug-in versions can simply overwrite old ones, even as part of
the build process. This is obviously the most convenient setup, since you can configure your
IDE to build the `.jar` artifact directly into the plug-in directory.

In other configurations, the existing `.jar` file may be locked, in which case you must first
move that file to another location or delete it.


