---
layout: default
title: Scalatron Pluggable Games
subtitle: Draft Architecture Documentation
---

WARNING: this is a draft documenting a work-in-progress!

# How To Write A Game Plug-In For Scalatron

* Create a new project, say `MyGame`
* Possibly by copying and renaming the `ScalatronDemoGame` template on Github
* It will currently require the following dependencies:

    ScalatronCore.jar
    scala-library-jar (Scala 2.9.1)
    akka-actor-2.0.jar (Akka 2.0)

* Configure it to generate an artifact called `MyGame.jar` into the Scalatron `/bin` directory
* implement a class `scalatron.myGame.Game` that implements the `scalatron.core.Game` trait, like so:

    package scalatron.myGame
    case object Game extends scalatron.core.Game {
        ...
    }

* implement a class `scalatron.GameFactory`, like so:

    package scalatron
    class GameFactory { def create() = scalatron.myGame.Game }

* then flesh out the functionality of your Game implementation, starting with the method `runVisually()`




