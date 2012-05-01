SCALATRON - Learn Scala With Friends
http://scalatron.github.com - Twitter: @scalatron - scalatron@hotmail.com
This work is licensed under the Creative Commons Attribution 3.0 Unported License.

Contributor Documentation
Version 0.9.9 -- updated 2012-05-01



# Design Goals:

* make it as easy as possible for users to create, compile and publish a working bot
* avoid any coupling between server and bots - there are no shared types
* simple game rules that allow for complex strategies




# Design Notes:

*   separation into game (scalatron.botwar) and simulator (scalatron.simulator)
    was intended to eventually allow for additional game types to be implemented
    within the same infrastructure.

*   it would of course be possible to provide a richer API for plug-ins, e.g.
    using a trait that contains the opcodes as methods ("React" would become:

        trait Bot {
            def react(entity: String, view: Array[Char], energy: Int)
        }

    However, the intent of this project is to serve as a teaching tool and the
    current setup has the advantage of completely eliminating any coupling between
    the game server and the bots - no files to include, no dependencies that can
    go wrong etc. Having to parse the strings and handle the cases may be a
    downside, but that is where the presenter begins introducing Scala
    programming concepts.




# Coding Conventions:

*   roughly follows the Scala Style Guide (http://www.codecommit.com/scala-style-guide.pdf)
*   4 spaces per tab (I know, the Scala Style Guide calls for two, but I prefer more obvious indentation)
*   I use a large screen and do not mind long lines



# Known Issues:

*   the whole thing was hacked together pretty quickly. Probably needs quite a
    bit of additional refactoring. :-) Especially the renderer and the collision
    code are still quite spaghetti-like and could benefit from some attention.

*   there is no time or space constraint on the user code invoked with the control
    functions. It is the responsibility of the players not to write unreasonable
    code. The next game cycle will not occur until the last plug-in has responded.

*   it can happen that player bots are placed such that they are fully enclosed by
    walls, obviously harming the player's chances of winning. One possible solution:
    when placing wall segments, generate exclusion rectangles for them. Player bots
    cannot be placed within these rectangles. The rectangles are retained only during
    map generation. However: based on experience so far, this is extremely rare, so
    this is of relatively low priority.

*   player colors are hash-based and are not actually guaranteed to be unique.

*   initial profiling of the code revealed that on an 8 CPU machine, about 30% of
    the time was spent computing updates, 35% drawing (of which the majority was
    in clearing the background) and 35% copying the final image to the screen.
    Simple double-buffering with copy-to-screen in a background-thread was added.
    The background clearing should probably also move into either the image copy
    thread or another background thread (with triple buffering). But not sure yet
    how to do this best on lower-CPU-count machines

*   apparently, when the simulator is paused for a long time (e.g. computer goes to sleep),
    the renderer (game results?) can generate time-based overlay alpha values that
    are out of range.





# Ideas for Contributions / Improvements for Versions 2+:

*   make game implementations (such as BotWar) pluggable.

*   constrain users' CPU usage, maybe by using Akka Actors with timeouts
    instead of the parallel collections (which have no timeouts).

*   allow players to determine the appearance (colors, shape) of their bots
    by issuing an appropriate control function response opcode.

*   to speed up the pace of simulation it may be desirable to add a "update the screen
    only every N cycles" option, plus a set of associated keyboard commands.

*   mouse control: click on a bot to identify it; click on a ranking panel to highlight
    the associated bot. Zoom in/out; pan; etc.

*   constrain the number of mini-bots (slaves) a master bot may spawn. Maybe to N = 10.

*   when generating the arena, take the screen aspect ratio into account to generate default
    -x and -y values. Currently, it can happen that the default values for window size plus
    the (independently chosen) default values for arena size result in large blank areas on
    the screen.

*   we may want a mechanism for bots to communicate with each other even if no state is held
    in the control function. Options:
    *   add a "SetShared()" opcode that updates parameters received by all bots?
    *   add a "SetOn(target=name/id,key=value,key=value,...)" or
        "SendMessage(target=name/id,key=value,...)" opcode that gives bots the ability to
        communicate by setting each other's state parameters?
    *   add a "children=xxx" parameter to "React()" that lists all child bots?

* Browser UI: allow editing of multiple source files in multiple tabs
