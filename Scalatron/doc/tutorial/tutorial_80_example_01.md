---
layout: default
title: Example Bot #1 - The Reference Bot
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_20_bot_10.html' data-next='/tutorial/tutorial_80_example_02.html' />

# Example Bot #1: The Reference Bot

<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_80_example_01_bot.scala">Load into Editor</button>

The "Reference Bot" is quite a sophisticated bot that is nevertheless readily understandable
based if you've worked your way through the tutorial.

## Strategy

The master bot's primary objective is to harvest energy by eating approach edible plants and
pursuing edible beasts, while avoiding poisonous plants and predatory beasts. It achieves this
by examining its view and building a *direction value map*. This map assigns an attractiveness
score to each of the eight available 45-degree movement directions. After building the map,
the bot selects the most promising direction and issues an appropriate `Move()` command.

In addition, the bot sends out aggressive missiles (mini-bots) when it spots an enemy master
and defensive missiles when it spots an enemy slave. These missiles initially head towards
their designated target, but opportunistically switch into a pursuit mode when they detect
a targetable entity. Once they get close enough to a target, then explode.


## Implementation

The implementation uses a small framework to simplify the processing of incoming and the
assembly of outgoing commands. Most of the components of this framework were introduced over
the course of the tutorial.

The master bot uses the following state parameters:

* `dontFireAggressiveMissileUntil`
* `dontFireDefensiveMissileUntil`
* `lastDirection`

The mini-bots use the following state parameters:

* `mood`: Aggressive | Defensive | Lurking
* `target`: remaining offset to target location
