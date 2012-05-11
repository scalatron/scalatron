---
layout: default
title: Bot #12 - Handler Object
---
# Bot #12: Handler Object

## Objective

In preceding bot versions we occasionally held some state inside the Bot object, which contained
our control function.


## Source Code v2

    // omitted: class ControlFunctionFactory
    // omitted: class CommandParser
    // omitted: class View

    class ControlFunction() {
        // this method is called by the server
        def respond(input: String): String = {
            val (opcode, params) = CommandParser(input)
            opcode match {
                case "Welcome" =>
                    welcome(
                        params("name"),
                        params("path"),
                        params("apocalypse").toInt,
                        params("round").toInt
                    )
                case "React" =>
                    react(
                        View(params("view")),
                        params("entity"),
                        params
                    )
                case "Goodbye" =>
                    goodbye(
                        params("energy").toInt
                    )
                case _ =>
                    "" // OK
            }
        }

        def welcome(name: String, path: String, apocalypse: Int, round: Int) = ""

        def react(view: View, entity: String, params: Map[String, String]) =
            if( entity == "Master" ) reactAsMaster(view, params)
            else reactAsSlave(view, params)

        def goodbye(energy: Int) = ""

        def reactAsMaster(view: View, params: Map[String, String]) = ""

        def reactAsSlave(view: View, params: Map[String, String]) = ""
    }

## What is going on?

The setup is quite staright-forward: instead of handling the invocations from the server
inside the `respond()` method, we parse the command and feed it through a `match`/ `case`
pattern matcher. The pattern matcher contains one handler for each of the opcodes the
server may send (we ignore unknown opcodes).

Each handler extracts frequently-used parameters from the parsed parameter maps and
feeds them as arguments to an opcode-specific handler method:

* the `welcome()` method handles the `Welcome` opcode
* the `react()` method handles the `React` opcode
* the `goodbye()` method handles the `Goodbye` opcode

Within the `react()` handler method we inspect the entity for which a response is
required and then branch into the appropriate entity-specific handler:

* the `reactAsMaster()` method handles `React` invocations for the (master) bot
* the `reactAsSlave()` method handles `React` invocations for mini-bots


