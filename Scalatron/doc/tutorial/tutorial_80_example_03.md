---
layout: default
title: Example Bot #3 - The Debug Status Logger
---
<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_80_example_02.html' data-next='/tutorial/tutorial_90_cheatsheet_00_string.html' />

# Example Bot #3: The Debug Status Logger

The "Debug Status Logger" illustrates how to use the `Log()` command to store debug information
in the state parameters of your bot. This state information can be inspected in the debugger
while the bot is running in a sandbox.


## Try It Out

To try it out, click <button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_80_example_03_bot.scala">Load into Editor</button>
to load the code into the editor. Then click the button above the editor labeled
**Run in Sandbox**. This will upload your bot code to the server, build it there into your
local workspace and then launch a private, sandboxed game on the server that contains just
your bot.

If your bot compiled without errors, a panel will slide out on the right that lets you inspect
the state of the sandboxed game. Click **Run** in the toolbar of that panel to begin stepping
through the simulation.

As the simulation progresses, you will see the debug output of your master bot in the
area labeled **Log** at the bottom.