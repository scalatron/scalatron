---
layout: default
title: Scalatron Architecture
subtitle: Developer Documentation
---

# About Scalatron

Scalatron is an educational resource for groups of programmers that want to learn more about
the Scala programming language or want to hone their Scala programming skills. It is based on
Scalatron BotWar, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other.

The documentation, tutorial and source code are intended as a community resource and are
in the public domain. Feel free to use, copy, and improve them!



# Design Goals

Scalatron is intended as a fun way to learn Scala, both on your own and together with a group of
friends or colleagues. It should be easy to get started, yet provide enough depth to keep players
engaged for a while.

This intention leads to the following design goals:

* make it as easy as possible for users to create, compile and publish a working bot
* make it as easy as possible for organizers to set up a tournament for multiple users
* provide simple game rules that allow for sophisticated strategies (emergent complexity)

This implies a few secondary design goals:

* provide a complete package that can be installed with a single download & unzip step
* avoid the need for any initial configuration - double-clicking the app should just work
* avoid any coupling between server and bots - there are no shared types
* the player should be led along the path to a sophisticated bot with an integrated tutorial
* experimentation and exploration is encouraged - nothing should break no matter what a bot does

Some of this is achieved in version 1.0, but many areas that could use some polishing remain.



# Architecture Overview

Scalatron is a client-server application. The server, which can be installed centrally or on
a player's own machine, continually runs tournaments between bots, which can be either
uploaded through the file system to a shared directory or built from sources on the server
through the browser UI.


     -----------------------------------------           -----------------------------
    |      Scalatron Server Application       |         |  Scalatron Client (Browser) |
     -----------------------------------------           -----------------------------
    |                                         |         |                             |
    |  -------------------------------------  |         |  -------------------------  |
    | |      Embdedded Web Server           | |         | |    HTML / JavaScript    | |
    |  -------------------------------------  |         |  -------------------------  |
    | |                                     | |         | |                         | |
    | |  ---------------   ---------------  | |         | |   ExtJS: Ajax calls to  | |
    | | |  Servelets    | |  Resources    |<--------------|   Scalatron REST API    | |
    | | | (static HTML) | | (RESTful API) | | |         | |                         | |
    | |  ---------------   ---------------  | |         |  -------------------------  |
    | |         |                 |         | |         |                             |
    |  -------- | --------------- | --------  |          -----------------------------
    |           |                 |           |
    |  ---------V-----------------V---------  |
    | |    Scalatron Kernel / Scala API     | |
    |  -------------------------------------  |
    | |         |                 |         | |
    | |  -------V-------   -------V-------  | |
    | | |    Compile    | |   Tournament  | | |
    | | |    Service    | |      Loop     | | |
    | |  ---------------   ---------------  | |
    | |                           |         | |
    | |          -----------------|         | |
    | |         |                 |         | |
    | |  -------V-------   -------V-------  | |
    | | |     Game      | |      Game     | | |
    | | |  Simulation   | |    Renderer   | | |
    | |  ---------------   ---------------  | |
    | |                                     | |
    |  -------------------------------------  |
    |                                         |
     -----------------------------------------


## The Server

The server is a stand-alone application written in Scala. It consists of two
primary components, which we will dissect in more detail further down:

* The **Scalatron server kernel**. This component contains the plug-in loading mechanism, the
  background compile service, the game simulator, versioning and sharing tools, and the tournament
  loop. All of this functionality is exposed as a Scala API via `trait Scalatron` and its collection
  of support traits.
* An **embedded web server**, currently based on Jetty. The web server serves the browser-based user
  interface and exposes certain sections of Scalatron's native Scala API as a RESTful web API.



## The Client

The client is a browser front-end served up by the embedded web server. It consists of a minimal set of
static pages (such as the login page), which then use JavaScript to access a RESTful web API exposed by
the server.




# Server Architecture


## Server Components

The Scalatron server, which in compiled form is available in the Java archive `/bin/Scalatron.jar`,
provides a range of services which execute concurrently:

* A **background compilation service** that waits for compile jobs referencing a directory containing Scala
  source files. When a job is received, the sources are compiled into `.class` files and linked into a
  Java archive `.jar` files, which can subsequently be loaded by the plug-in manager. The builds results,
  primarily consisting of information about errors and warnings generated by the compiler, are sent back
  to the caller. The compilation service is implemented via an Akka `Actor`.
* A **web server** that serves static pages and exposes the Scala API via a RESTful web API. The web
  server uses additional threads to service client requests, either streaming files or converting invocations
  of REST resources into Scalatron API method calls.
* A **tournament loop** occupies the main thread. It runs continually or for a configurable number of
  rounds and executes one game after another, collecting results into a tournament leader board. The
  tournament loop internally uses parallel computation to evaluate all entity control functions concurrently
  and to render the game display in four concurrently executing passes.


## Start-up and Initialization

When the server application is launched, it proceeds to initialize itself as follows in its `Main` class:

* It creates an Akka `ActorSystem` that will be used by multiple components for concurrent processing.
* It initializes the `Scalatron` kernel, obtaining a reference to an instance of the `Scalatron` API trait.
* It starts the `Scalatron` kernel, which launches the background compile service.
* It starts the web server, which begins listening for browser connections.
* It launches a browser window pointing to the web server's port.
* It enters the tournament loop, either headless or with visual output. This loop continues running until
  a configured number of rounds was played or until the user manually exits the application.

Note that the Scalatron kernel, in addition to the thread pool held by the Akka `ActorSystem`, creates a
second thread pool specifically for the execution of untrusted code. All operations executing within this
thread pool are monitored by a custom `SecurityManager`, which allows the server to isolate plug-in code
into sandboxes to prevent it from performing undesirable operations on the server.


## The Compile Service

Scalatron allows players using the browser-based user interface (Scalatron IDE) to build bot plug-ins
simply by clicking a button. The process that makes this possible is as follows:

* When the user clicks **Build**, **Run in Sandbox** or **Publish into Tournament** in the browser,
  the source code currently active in the embedded editor is uploaded to the server.
* The source code files are patched by the server to embed a package statement containing the name of
  the submitting user. This is necessary to prevent name collisions in the cached compiler instance
  when multiple users submit bot code that contains the same class names (which is very common).
* The patched source files are sent to the `CompileActor`, which passes them to a cached instance of
  a Scala compiler. The first compilation is takes relatively long (15-20 seconds), but subsequent
  compilations are very fast (as short as 100 milliseconds).
* If no errors were detected, the generated `.class` files are packaged into a Java archive (`.jar`)
  file, which is then placed into the user's server-side workspace.
* The compiler error and warning messages are finally post-processed (for example to adjust line numbers
  to take the patched-in package statement into account) and returned to the caller as a build result object.



## The Game Simulation

The game is based on a simulation that consists of an (immutable) game state and dynamics that
operate on the game state. The simulation is advanced by applying the dynamics to the state,
generating a new state.

The dynamics themselves are nested:

* The outer `Dynamics` instance requests the outputs of all entity control functions (concurrently)
  and assembles them into a command list, which it combines with the incoming game state into an augmented
  game state.
* The inner `AugmentedDynamics` instance receives the augmented game state (pure game state plus a
  command list) and proceeds to apply the commands as well as the natural dynamics of the game to the
  pure game state, generating a new pure game state.

A game round is run by sequentially executing a configurable number of simulation steps (applying `Dynamics`
to the state). After each step (and executing concurrently with the next step) a callback is invoked that
receives the updated step. Rendering is performed via this callback mechanism, which means that rendering
of the prior state executes concurrently with the computation of a successor state.



## Rendering

Rendering of updated game states is triggered by a callback invoked from within the simulation loop.
The renderer simply takes the given game state and draws it into a `BufferedImage`, which is them
blitted to the screen into the `Graphics2D` context of an AWT `Frame`.

Rendering executes in a multi-stage pipeline whose steps execute concurrently. Each step renders some
portion of the user interface (background, entities, scores, etc.) into an image buffer, which is then
shifted to the next stage. This multi-stage pipelining lets rendering use multiple CPU cores to accelerate
the drawing, at the expense of increased simulation-to-screen latency. Since the screen updates are purely
for visual inspection purposes and no human intervention is required (it's a bot programming game, after
all), this latency is not of concern.






## The Web Server

While the web server receives a reference to the shared Akka `ActorSystem`, the current implementation
based on Jetty does not use this reference. The intention is to eventually replace Jetty with an Akka/Scala
embedded web server, such as Spray.

The web server servers a static entry page, index.html, which then allows the user to log-in as a
user or as Administrator, which, after authentication, either leads to an editor page or to an
administration page.

The web server also exposes a range of resources that implement the RESTful API sepecification.
These resources are accessed via Ajax calls by the JavaScript code running in the browser and
by Scala code in the Scalatron Command-Line Interface (CLI).





# Client Architecture

The client consists of a collection of web pages, some of which contain fairly complex JavaScript code
to implement the user interface. Of particular interest is the page that implements the Scalatron IDE.
It combines several panels (Tutorial, Editor, Sandbox, Console) to enable a player to do all bot
development in the browser, leveraging server-side services for more complex tasks like compilation and
simulation.

The client uses Sencha's *ExtJS* to generate the user interface and support the Ajax invocations of
the server's RESTful web API. It also uses the *ACE* editor component to provide syntax-coloring and
(for a browser) fairly sophisticated code editing capabilities.



# Contributing

If you are interested in contributing, the best starting point is to [fork the project on Github](http://github.com/scalatron/scalatron)
and to look at [the list of open issues](http://github.com/scalatron/scalatron/issues?state=open).
Before embarking on something major, you could [contact the maintainer](mailto:scalatron@hotmail.com)
and ask for feedback or tips on where to get started.

But of course Scalatron is open source and in the public domain, so you are free to do whatever you want.



# Coding Conventions

The Scala parts of Scalatron roughly follow the [Scala Style Guide](http://www.codecommit.com/scala-style-guide.pdf),
with the following notable idiosyncrasies, which I'd request you respect, if possible, when sending pull requests:

* instead of the two spaces called for by the Scala Style Guide, Scalatron uses **four spaces per tab** and so
  indentation is universally four spaces. This is simply because I find it much more ergonomic to have very
  obvious indentation, not least because...
* I use a large screen (30") and do not mind long lines.

As a final note, I use Jetbrains' *IntelliJ IDEA* for development (Scala and JavaScript). One of its nice features
are inspections, which tell you about many things that might be suboptimal in your code, such as discrepancies
in using parentheses for arity-0 methods etc. Highly recommended.
