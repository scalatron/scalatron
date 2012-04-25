SCALATRON - Learn Scala With Friends
http://scalatron.wordpress.com - Twitter: @scalatron / #scalatron
This work is licensed under the Creative Commons Attribution 3.0 Unported License.

Developer Documentation - How to Build a Scalatron Release
Version 0.9.8 -- updated 2012-04-22


# How to Assemble an End-User Release

* there are two primary content tress:
    * development `/Scalatron/scalatron-src/Scalatron/`
    * distribution `/Scalatron/scalatron-game/Scalatron/`

* in the development tree, build:
    * Scalatron, yielding `/Scalatron/scalatron-src/Scalatron/bin/Scalatron.jar`
    * ScalatronCLI, yielding `/Scalatron/scalatron-src/Scalatron/bin/ScalatronCLI.jar`
    * IntelliJ IDEA project files are provided for both projects

* copy binaries:
    * from `/Scalatron/scalatron-src/Scalatron/bin/*.jar`
    * to `/Scalatron/scalatron-game/Scalatron/bin`

* verify that an example Reference bot is in `/Scalatron/scalatron-game/Scalatron/bots/Reference/ScalatronBot.jar`

* run ScalaMarkdown.jar to generate HTML documentation from Markdown versions:
    * `ScalaMarkdown.jar /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/doc/markdown /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/doc/html`
    * `ScalaMarkdown.jar /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/devdoc/markdown /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/devdoc/html`
    * `ScalaMarkdown.jar /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/doc/tutorial /Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/webui/tutorial`

* copy documentation directories `html` and `pdf`:
    * from `/Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/doc`
    * to `/Scalatron/scalatron-game/Scalatron/doc`

* copy `License.txt` and `Readme.txt`
    * from `/Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/`
    * to `/Scalatron/scalatron-game/Scalatron/`

* copy `/samples` directory:
    * from `/Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/samples`
    * to `/Scalatron/scalatron-game/Scalatron/samples`

* delete the `users` directory `/Scalatron/scalatron-game/Scalatron/users`; the app will auto-create this

* copy `/webui` directory:
    * from `/Users/dev/Scalatron/scalatron-src/Scalatron/src/Scalatron/webui`
    * to `/Scalatron/scalatron-game/Scalatron/webui`

* build the `scalatron-game` .zip file:
    * zip up the directory `/Scalatron/scalatron-game/Scalatron/`
    * and rename the resulting .zip file to e.g. `scalatron-game-0.9.7.9.zip`



# How to Assemble a Developer (Source) Release

* delete all intermediate files not intended for redistribution, including:
    * `/Scalatron/scalatron-src/Scalatron/src/Scalatron/out`
    * `/Scalatron/scalatron-src/Scalatron/src/ScalatronCLI/out`
    * `/Scalatron/scalatron-src/Scalatron/src/ScalaMarkdown/out`

* build the `scalatron-src` .zip file:
    * zip up the directory `/Scalatron/scalatron-src/Scalatron/`
    * and rename the resulting .zip file to e.g. `scalatron-src-0.9.7.9.zip`

