SCALATRON - Learn Scala With Friends
http://scalatron.github.com - Twitter: @scalatron - scalatron@hotmail.com
This work is licensed under the Creative Commons Attribution 3.0 Unported License.


# READ ME

## About Scalatron

Scalatron is an educational resource for groups of programmers that want to learn more about
the Scala programming language or want to hone their Scala programming skills. It is based on
Scalatron BotWar, a competitive multi-player programming game in which coders pit bot programs
(written in Scala) against each other.

The documentation, tutorial and source code are intended as a community resource and are
in the public domain. Feel free to use, copy, and improve them!


## Quick Start

* download the Scalatron distribution, e.g. from http://scalatron.github.com
* unzip the compressed file to a local directory, e.g. `/Scalatron`
* launch the game server by double-clicking the application, e.g. at `Scalatron/bin/Scalatron.jar`
* this should automatically open a browser and point it to e.g. `http://localhost:8080`
* log in as `Administrator`, create a user account for yourself
* log in as that user, which will take you into a browser-based code editor
* click "Run in Sandbox" to build your bot and run it in a private sandbox game
* click "Publish into Tournament" to build your bot and publish it into the tournament
* once you know your way around, invite some friends for a bot coding tournament and have fun!


## Learning More

* browse into the `/Scalatron/docs/pdf/` directory
* here you will find a collection of useful documents, including the following:
    * `Game Rules`      -- describes the BotWar game state & dynamics
    * `Protocol`        -- describes how server and bots interact for the BotWar game
    * `Player Setup`    -- how set up your local working environment to build bots
    * `Server Setup`    -- how to configure the game server
    * `Tutorial`        -- how to code a bot in Scala


## For Developers

* go to http://github.com/scalatron to download the Scalatron source code
* check out developer documentation, in particular the API and the doc on pluggable games
* send bugs, suggestions and fixes to `scalatron@hotmail.com`




## Version History

### Version 1.1.0.2 -- 2012-05-23

* Adds support for server-side compilation of bots written in Java. This relies on Java's `tools.jar`,
  which may not be available on all systems. Works with Java 1.6 on MacOSX, fails on Windows.



### Version 1.1.0.1 -- 2012-05-22

* Adds support for bots written in Java. Implement a simple bot by placing this code in a file `ControlFunction.java`:
  public class ControlFunction { final public String respond(String input) { return "Status(text=Written in Java)"; } }



### Version 1.1.0.0 -- 2012-05-22

* Adds experimental support for additional games beyond BotWar which can be loaded from plug-ins. For details,
  please refer to the new document `Scalatron Pluggable Games`. Use the command line option `-game name` to
  load a game other than BotWar.


### Version 1.0.1.2 -- 2012-05-21

* Protocol change: `Welcome` opcode no longer provides parameter `path`.


### Version 1.0.1.1 -- 2012-05-21

* Minor fix to facilitate migration of existing user accounts (created with 1.0.0.2 or earlier) to git-based versioning.


### Version 1.0.1.0 -- 2012-05-20

* Version control now uses Git. See "intermediate" path documentation in Scalatron Player Setup guide. Thanks Charles O'Farrell (@charleso)!
* New Scalatron CLI command: `publish`, to publish an unpublished bot into the tournament loop.
* Fixed issue #30: browser UI now uses Scalatron REST API to load initial source file.
* Now state property: `collision`, reports when a move failed due to a collision; Protocol doc has details. Thanks U.G. Dietrich!
* Minor fix to how class paths are reported on Windows (eliminated leading slash from `/C:/Docs...`).


### Version 1.0.0.2 -- 2012-05-10

* there is now an experimental "secure mode" for Scalatron, which can be enabled with the command line argument
  "-secure yes". This has the following effects:
   (a) Plug-ins run in a sandbox and can no longer access the file system, network, etc. on the server.
       Note that plug-ins can also no longer access the file system for logging purposes.
   (b) Each plug-in can only have a fixed maximum number of slave entities in the game at any one time;
       the default value is 20, and can be adjusted with the argument "-maxslaves int"
   (c) not yet quite working: timeouts for plug-in control functions.


### Version 1.0.0.1 -- 2012-05-09

* added a favicon.ico to remove warning about missing file
* fixed issue #39: server fails to initialize correctly when launched from directory containing spaces.
* fixed issue #37: server fails to delete source directory on Windows because of retained file lock.
* added `Scalatron Troubleshooting` document


### Version 1.0.0.0 -- 2012-05-08

* improved the documentation conversion from markdown to HTML, adding layouts, stylesheets and images


### Version 0.9.9.5 -- 2012-05-06

* version control is now available in the browser UI via "Save..." and "Revert..." buttons. Thanks @daniel_kuffner!
* plug-ins can now optionally be isolated into a sandbox to prevent them from executing malicious code on the server.
  To enable this experimental feature, use the command line argument "-sandboxed yes". Note that plug-ins can no
  longer access the file system for logging purposes. (updated 2012-05-09: command is now "-secure yes").


### Version 0.9.9.4 -- 2012-05-04

* RESTful web API and command line client now support these additional commands:
    /api/users/{user}/versions/{versionId}  GET     Get Version Files
    /api/users/{user}/versions/{versionId}  DELETE  Delete Version
* fixed: on Windows in IE and Firefox, code in the editor appeared truncated to a single line (issue #22).
* fixed: sign-out (sometimes silently) failed because it connected to the wrong REST resource (issue #27).
* fixed: in IE and Firefox links in the tutorial opened in a new tab instead of the tutorial panel (issue #21).


### Version 0.9.9.3 -- 2012-05-02

* new opcodes: "MarkCell()", "DrawLine()"; see Scalatron Protocol docs for details. Thanks Joachim Hofer, @johofer!
* name duplication bug fixed; docs for "React()" opcode fixed. Also thanks to @johofer!


### Version 0.9.9.0 - 0.9.9.2 -- 2012-04-26

* restructured for move to github
* bot processing ported from parallel collections to Akka
* rendering code ported from double-buffering with raw threads to quad-buffering with Akka
* RESTful API: resource "/users/{user}/sandbox" is now "/users/{user}/sandboxes"
* Scalatron API: trait SandboxState is now accessible through new trait Sandbox
* minor changes to the plug-in loading code to deal with the results of various jar assembly tools
* installation directory is now detected via the class path, not the Java user directory (fixes HTTP 500 error)


### Version 0.9.8 -- 2012-04-22

New features:
* in the Scalatron IDE:
    * now has an integrated tutorial, mirroring the tutorial content of the PDF.
    * spinning `busy` icon when building.
    * warning when loading new source code over changed content in the editor.
* there now is a Scalatron CLI (Command Line Interface), run `ScalatronCLI.jar` with `-help` for info.


### Version 0.9.7 -- 2012-04-14

New features:
* the Scalatron server now runs a web server that serves a browser-based development environment
  (a simple "Scalatron IDE"). Thanks to Daniel Kuffner (@daniel_kuffner), who built the browser UI!
* the server can now run a variety of sandboxed games in "headless" mode.
* the server now maintains a /samples directory containing the tutorial bot samples.
* the server now maintains /users/{user}/versions directories.

API changes:
* New **Scalatron Scala API** -- the Scalatron server now exposes a Scala API that provides access to most
  important states and operations. Specifically: for managing (web) users; for updating source
  code in a local workspace; for building that code into bots; for launching sandboxed, private
  game rounds that load the local bot version; for publishing the local bot version into the
  tournament plug-in directory; for creating, deleting and listing sample bots; and for tracking
  the state of the tournament loop.
* New **REST API** -- the Scalatron server also exposes its API as a RESTful web API. You can use
  this API to create your own tools, like a tournament leaderboard tracker, integration into
  your IDE or writing your own client IDE. For details, see the *Scalatron API Documentation*
  document. Thanks to Andreas Flierl (@asflierl) and Joachim Hofer (@johofer) for reviewing the
  API draft. Thanks to Daniel Kuffner (@daniel_kuffner) for the implementation.

Protocol changes:
* new opcode: "Set(key=value,key=value,...)". The key/value pairs are appended to the key/value
  list of the "React" opcode passed to the control function for bots and mini-bots. This allows
  bots to store their state on the server more conveniently. The browser UI will display these
  key/value pairs in a watch list in the debug window.
* new opcode: "Log(text=line 1\nline 2\nline 3)". Stores the given string as debug information
  on the entity (using the state property `debug`), which can be retrieved via the Scalatron API;
  or the REST API; or displayed in the browser UI as debug information. Calling `Log(text=xyz)` is
  a shorthand for `Set(debug=xyz)`.
* modified opcode semantics: "Spawn(...)" now accepts arbitrary additional parameter key/value
  pairs which are automatically set as the slave's initial state.
* modified opcode parameter: "Spawn(direction=int:int)" now expects a direction of the format "x:y"
  (e.g. "1:-1"). Previously, it expected two parameters, "dx" and "dy".
* modified opcode parameter: "Move(direction=int:int)" now expects a direction of the format "x:y"
  (e.g. "1:-1"). Previously, it expected two parameters, "dx" and "dy".
* modified opcode parameter: "React(master=int:int)". The server now invokes "React" with the
  property "master" to indicate the relative position of the master bot. The property has the
  format "x:y" (e.g. "-8:9"). Previously, it passed two separate parameters, "dx" and "dy".
* modified opcode parameter: "React(name=int:int)". The server now invokes "React" with the
  property "name", which is either the plug-in name (for the master bot) or the name passed to
  "Spawn()" (for mini-bots). This property replaces the previously used `entity`.
* additional opcode parameter: "React(generation=int)". The server now invokes "React" with the
  property `generation` to indicate the generation of the bot, with 0 (zero) corresponding to
  the master bot and 1, 2, ... corresponding to spawned mini-bots. Use this property to distinguish
  master and slave bots (previously, this was done by testing the parameter `entity` against the
  string "Master").
* deprecated opcode: "SetName(...)" was deprecated. Use "Spawn(name=...)".
* the opcode `Status` now sets the state property `status`, which is also what the renderer reads
  to display the bot status on the screen. So `Status(text=xyz)` is a shorthand for `Set(status=xyz)`.

Command line arguments:
* command line options now accessible through -help, are no longer displayed by default
* new option: -help             print command line options
* new option: -verbose yes|no   print verbose output (default: no)
* new option: -headless yes|no  run without visual output (default: no)
* new option: -browser yes|no   open a browser showing Scalatron IDE (default: yes)
* new option: -rounds <int>     run this many tournament rounds, then exist (default: 50)
* new option: -maxfps <int>     maximum frame rate (to reduce CPU load; default: unlimited)
* new option: -port <int>       port to serve browser UI & REST API at (default: 8080)
* new option: -webui <dir>      directory containing browser UI (default: ../webui)
* new option: -users <dir>      directory containing browser UI workspaces (default: ../users)
* new option: -samples <dir>    directory containing example bots (default: ../samples)
* new option: -perimeter <option> arena perimeter: none, open, or closed (default: closed)
* new option: -walls <int>      count of wall elements in arena (default: x*y/300)
* new option: -zugars <int>     count of good plants in arena (default: x*y/250)
* new option: -toxifera <int>   count of bad plants in arena (default: x*y/350)
* new option: -fluppets <int>   count of good beasts in arena (default: x*y/350)
* new option: -snorgs <int>     count of bad beasts in arena (default: x*y/500)

Bug fixes:
* before this version, the mini-bot view was accidentally not sent through an occlusion pass. Fixed.
* before this version, the occlusion computation was pretty funky. Fixed.
* before this version, when sibling mini-bots collided, they annihilated instead of just going "bonk". Fixed
* before this version, the direction of up/down and was reversed in the sample code for `class XY` in the tutorial. Fixed.
* before this version, explosion damage affecting Snorgs (bad beasts) was unlimited. Fixed, thanks to Ian Calvert (@IanCal).
* on Windows, the tournament display window apparently obscured the Task Bar. A bug fix proposal
  sent in by a user (use Frame.setExtendedState() with Frame.MAXIMIZED_BOTH) was added. Hopefully
  this fixed the issue - don't have a Windows machine so I cannot test it. Feedback appreciated.

Other changes:
* a License.txt file was added and the license was clarified as follows:
  "This work is licensed under the Creative Commons Attribution 3.0 Unported License."
* every bot and mini-bot now holds a debug output property which is cleared before each
  control function invocation.
* if a player entity (bot or mini-bot) causes an error while one of its commands is being
  processed, an error message is appended to its debug output.

Documentation changes:
* the *Scalatron Tutorial* now has a chapter *Debugging Your Bot*
* the *Configuring Your IDE* guide is now called *Scalatron Player Setup*
* the *Scalatron Player Setup* guide now explains how to use SBT -- thanks, Joachim Hofer (@johofer)!
* the *Scalatron Player Setup* guide now explains how to work from the command line -- thanks, Lauri Lehmijoki (@laurilehmijoki)!
* the *Scalatron Player Setup* guide now includes a link to an SBT template on github -- thanks, David Winslow (@godwinsgo)!


Known issues:
* when slave bots are informed about the relative position of their master bot via the `master`
  parameter in `React`, the offset provided is the shortest offset assuming a toroidal, open arena.
  If perimeter walls are present and a mini-bot is at the opposite end of the arena from its master,
  this will direct it to run into the perimeter wall.






### Version 0.9.6 -- 2012-04-04 -- experimental / internal
### Version 0.9.5 -- 2012-04-01 -- experimental / internal
### Version 0.9.4 -- 2012-03-31 -- experimental / internal


### Version 0.9.3 -- 2012-03-28

App:
* added handler for "window closed" event (e.g. when user clicks on window close icon)
* added keyboard shortcut 'r': aborts current round, reloads all plug-ins and starts next round
* modified the logic for computing the default arena size. Before, wider arenas were
  generated for larger player counts, resulting in lots of whitespace on some screens.
  Now all default arenas are square. Use -x / -y command line options to customize.
* added parameter `apocalypse` to `Welcome` opcode which tells control functions for how
  many steps the round will run before it terminates. This allows bots to plan ahead.
  Remember, however, that master bots are only asked to `React` every second step!

Docs:
* Server Setup guide: added info on 'r' shortcut
* Game Rules: added info mini-bot energy depletion (one EU every fourth cycle)
* Protocol: added info on `apocalypse` parameter



### Version 0.9.2 -- 2012-03-26

App:
* explosions now affect Snorgs (bad beasts)
* optimizations for projected displays:
     * status messages emitted via Status(text=string) commands now appears in white
     * the line connecting player-of-interest's bot and score panel is now white
* added default case handlers to the try/catch blocks surrounding plug-in invocations

Docs:
* significantly expanded the "Player Setup" document
* fixed typos in the tutorial
* added this version history document



### Version 0.9.1 -- 2012-03-24

* provided a single complete ZIP file containing the entire package




## Included Software

### Ace (Ajax.org Cloud9 Editor)

* web-based, syntax-coloring editor



### Jetty

* embedded web server


