<div id='TutorialDocumentData' data-prev='/tutorial/tutorial_80_example_02.html' data-next='/tutorial/tutorial_80_example_04.html' />

# Example Bot #3: The Debug File Logger

This example illustrates how to log debug information to disk while your bot is running inside
a multi-player tournament on a central game server. One reason this crude debugging mechanism
is occasionally useful is that your code is executed remotely on another compute inside a
Java Virtual Machine that you have no access to. There is therefore no way to set breakpoints,
to single-step through your code or to examine the values of variables at run-time, as you
would with a local debugger.

Note that this debugging approach is a measure of last resort. If you are working in the
browser-based Scalatron IDE or if you are using a local IDE and debugging your bot in a
local server is sufficient, you can skim this example and focus only on the discussion of
the Scala code for its own sake.



## Debug Logging

Since your code is running alongside multiple other bots on the server, simply sending
output to the console via `println` is not a viable option, except for extremely rare
messages. Log output would scroll past way too quickly and no-one would be able to see
what is going on.

The recommended approach is therefore to log your debug output into a plug-in specific
log file. But where to place that file? Your plug-in will need to use a path that is
valid in the context where it is being executed (i.e., on the computer where the game
server is running). To keep things simple and to avoid having you hard-code paths into
your plug-in, the server provides the path of the directory from which your plug-in was
loaded as a parameter to the "Welcome" command:

    Welcome(name=string,path=string,round=int)

You can cache the `path` and `round` values for later in fields of your bot class.

A second issue is logging overhead. If every plug-in were to log lots of data every step
of every round for every entity (master bot and mini-bots), there is a risk that the
game server would experience significant slowdown and the experience would be spoiled
for everyone. It is therefore recommended that you log information only during certain
steps of the simulation (say, at step zero or every 100th step). It is up to you whether
you append multiple data points to a single log file or whether you generate separate log
files for each such log-relevant event.

The example code below illustrates how one might do this.


## Source Code

<button class="LoadCodeButton" style="visibility: hidden;" data-url="/tutorial/tutorial_80_example_03_bot.scala">Load into Editor</button>

    import java.io.FileWriter

    // omitted: class ControlFunctionFactory
    // omitted: object CommandParser

    class ControlFunction() {
        var pathAndRoundOpt : Option[(String,Int)] = None

        def respond(input: String): String = {
            val (opcode, paramMap) = CommandParser.apply(input)

            opcode match {
                case "Welcome" =>
                    // first call made by the server.
                    // We record the plug-in path and round index.
                    val path = paramMap("path")
                    val round = paramMap("round").toInt
                    pathAndRoundOpt = Some((path,round))

                case "React" =>
                    // called once per entity per simulation step.
                    // We check the step index; if it is a multiple of
                    // 100, we log our input into a file.
                    val stepIndex = paramMap("time").toInt
                    if((stepIndex % 100) == 0) {
                        val name = paramMap("name")
                        pathAndRoundOpt.foreach(pathAndRound => {
                            val (dirPath,roundIndex) = pathAndRound
                            val filePath = dirPath + "/" +
                                           name + "_" +
                                           roundIndex + "_" +
                                           stepIndex + ".log"
                            val logFile = new FileWriter(filePath)

                            logFile.append(input)   // log the input

                            // if we logged more stuff, we might want an occasional newline:
                            logFile.append('\n')

                            // close the log file to flush what was written
                            logFile.close()
                        })
                    }

                case "Goodbye" =>
                    // last call made by the server. Nothing to do for us.

                case _ =>
                    // plug-ins should simply ignore unknown command opcodes
            }
            ""      // return an empty string
        }
    }



## What is going on?

We added a field to the `ControlFunction` class that will hold the directory path and the round
index as an `Option[Tuple2]`:

    var pathAndRoundOpt : Option[(String,Int)] = None

Initially, the field will hold the value `None`. Once the server tells us what the actual
values should be, the field will hold a `Some` value storing the plug-in directory path and
the round index as a pair. We'll explain the reason for the explicit type specification later.

Instead of the earlier `if` clause that tested for just the "React" opcode, we now want to
distinguish multiple possible values (and potentially complain about unknown opcodes). So a `match`
expression is better suited. In it, we distinguish between the three opcodes "Welcome",
"React" and "Goodbye" (see the *Scalatron Protocol* documentation for details).

In the opcode handler for "Welcome", we extract the values the server gives us for the
directory path and the round index and update the field:

    val path = paramMap("path")
    val round = paramMap("round").toInt
    pathAndRoundOpt = Some((path,round))

In the opcode handler for "React", we extract the index of the current simulation step:

    val stepIndex = paramMap("time").toInt

and test whether it is evenly divisible by 100:

    if((stepIndex % 100) == 0) {

The conditional code will therefore be executed only every 100th simulation step.
In it, we extract the name of the entity for which the server is invoking the control function
(for a master bot this will be the name of the plug-in; for mini-bots it will be whatever the
plug-in told the server their name should be in `Spawn`):

    val name = paramMap("name")

we then test whether the optional value `pathAndRoundOpt` was set (using a `foreach`
instead of `match` or `if`, a technique we'll explain in just a second) and, if so, we
extract the plugin directory path and the round index into two local values `dirPath` and
`roundIndex` (using the extraction syntax for `Tuple2` we already saw earlier)

    val (dirPath,roundIndex) = pathAndRound

and then construct a log file name that incorporates all of the distinguishing elements
of the debug "event": the entity name, the round index and the step index. Since the file
resides in the plug-in's directory, there is no need to incorporate the plug-in name.

    val filePath = dirPath + "/" +
                   name + "_" +
                   roundIndex + "_" +
                   stepIndex + ".log"

We then create a `FileWriter` instance for the log file

    val logFile = new FileWriter(filePath)

Now we can log into the file whatever we want. The example just logs the command
the server sent, plus a newline (just so you know how):

    logFile.append(input)   // log the input
    logFile.append('\n')

and then closes the log file to flush any buffered contents to disk:

    logFile.close()

That's it. In "Goodbye" we don't need to do anything for the example. If we had kept a log
file open to log information across multiple steps, however, we'd put the `logFile.close()`
into that handler.



## Why is the type of `pathAndRoundOpt` explicitly specified?

Note that in the field declaration

    var pathAndRoundOpt : Option[(String,Int)] = None

we explicitly specify the type as `Option[(String,Int)]` rather than letting the compiler
infer it for us. The reason this is necessary is that on its own, the compiler would
(correctly) infer the type by looking at the initialization value and would conclude it
should be the type of the singleton object `None` (which could be written down as `None.type`).

This would later lead to a compile time error when we attempt to assign the updated `Some`
value, since that type does not match what is expected.

To make this work as intended, we need to tell the compiler that what we really want is for the
field to have the parent type of `None` and `Some`, which - taking the polymorphic type of
the wrapped value into account - is `Option[(String,Int)]`. Hence the explicit specification
of the field's type.



## What does `pathAndRoundOpt.foreach()` do?

The objective of that code is to test whether the `Option` value is a `None` or a `Some`
and, if it is a `Some`, to extract the wrapped value so we can work with it.

Let's first look at a "brute force", more procedural implementation of the same code:

    if(pathAndRoundOpt.isDefined) {
        val pathAndRound = pathAndRoundOpt.get
        /* ... */
    }

We can do the same with a `match` expression, which many Scala programmers consider preferable
compared to `if`:

    pathAndRoundOpt match {
        case Some(pathAndRound) => /* ... */
        case None => // do nothing
    }

But the code in the example illustrates another possibility: use `foreach`.
As the name implies, `foreach` is a method available on collection types that executes
a function for each element of the collection. The reason we can use this on `Option`
is that an `Option` value mimics a collection that is either empty (if the `Option`
value is `None`) or that contains a single element consisting of the wrapped value (if
the `Option` is `Some`). So the code we used:

    pathAndRoundOpt.foreach(pathAndRound => {
        val (dirPath,roundIndex) = pathAndRound
        /* ... */
    })

works because the closure `pathAndRound => {/*...*/}` is only executed if the
`Option` is a `Some`, i.e. if indeed the "Welcome" command was already received
and the directory path and round index have become available.

For the example shown here, the solution feels a bit contrived since there are really no
savings in terms of code size. However, the approach demonstrates a more general principle
that is extremely useful and in many situations much more elegant than `if` or `match`,
not least because it also extends to other collection methods like `map`, where the benefits
may be more evident. In `map`, we can provide a function that transforms the wrapped value
into a new value, resulting in an `Option` wrapping that new value without the need to handle
the `None` case *at all*.


