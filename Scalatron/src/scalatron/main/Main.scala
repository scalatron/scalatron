/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.main

import scalatron.Version
import scalatron.webServer.WebServer
import akka.actor._
import scalatron.scalatron.api.ScalatronOutward


/** The entry point for the game server application. Selects as specific game (BotWar)
  * and runs it in the Scalatron game server with the given command line arguments.
  */
object Main {
    /** We'll launch the given game into the Scalatron game server, plus we'll launch two
      * background threads: a background compile server and a web server that manages the
      * browser UI.
      */
    def main(args: Array[String]) {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Scalatron")
        println("Welcome to Scalatron " + Version.VersionString)
        if(args.length == 0) {
            println("use -help to list available command line parameters")
            println("e.g. java -jar Scalatron.jar -help")
            println("")
        } else
        if(args.find(_ == "-help").isDefined) {
            // print list of available command line parameters to the console
            println("java -jar Scalatron.jar [-key value] [-key value] [...]")
            println("  with the following parameter key/value pairs:")

            val completeArgList = cmdArgList ++ ScalatronOutward.cmdArgList ++ WebServer.cmdArgList
            completeArgList.foreach(p => println("   -%-22s  %s" format(p._1, p._2)))
            System.exit(0)
        }

        // convert the command line parameters into a map of key/value pairs
        val argMap = args.grouped(2).filter(_.length==2).map(a => (a(0),a(1))).toMap    // Map["-key" -> "value"]

        // find out if we should provide verbose output
        val verbose = true//(argMap.get("-verbose").getOrElse("no") == "yes")


        // prepare the Akka actor system to be used by the various servers of the application
        val actorSystem = ActorSystem("Scalatron")

        // start up Scalatron background services (e.g. compile service, which will use the actor system)
        val scalatron = ScalatronOutward(argMap, actorSystem, verbose)
        scalatron.start()

        // prepare (and start) the web server - eventually this should also use the Akka actorSystem (e.g., Spray?)
        val webServer = WebServer(actorSystem, scalatron, argMap, verbose)
        webServer.start()

        // pass control to the tournament game loop - runs either forever or for some rounds ("-rounds" cmdline arg)
        scalatron.run(argMap)

        // shut down the web server
        webServer.stop()

        // shut down the Scalatron background services (e.g. compile service)
        scalatron.shutdown()

        // shut down the Akka actor system
        actorSystem.shutdown()
    }

    val cmdArgList = Iterable(
        "help" -> "display these help options",
        "plugins <dir>" -> "plug-in base directory (default: ../bots)",
        "samples <dir>" -> "directory containing example bots (default: ../samples)",
        "rounds <int>" -> "run this many tournament rounds, then exit (default: unlimited)",
        "headless yes|no" -> "run without visual output (default: no)",
        "verbose yes|no " -> "print verbose output (default: no)",
        "cpuTime" -> "cpu time ratio compared to Reference"
    )
}


