---
layout: default
title: Example Bot #2 - The Tag Team
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_80_example_01.html' data-next='/tutorial/tutorial_80_example_03.html' />

# Example Bot #2: The Tag Team

<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_80_example_02_bot.scala">Load into Editor</button>

The "Tag Team Bot" illustrates how state parameters can be passed from a master bot to its
mini-bots and how mini-bots can use them to control their behavior.

## Strategy

The master bot spawns a collection of companion mini-bots at certain time intervals.
The mini-bots strive to remain at a configurable offset relative to their master bot.
The master bot simply runs in large circles.


## Implementation

The implementation uses a small framework to simplify the processing of incoming and the
assembly of outgoing commands. Most of the components of this framework were introduced over
the course of the tutorial.

The master bot uses the following state parameters:

* `heading`: direction it is currently traveling
* `lastRotationTime`: simulation time when the bot last rotated its heading
* `lastSpawnTime`: simulation time when the last mini-bot was spawned

The mini-bots use the following state parameters:

* `offset`: the desired offset from the master, as seen by the master
