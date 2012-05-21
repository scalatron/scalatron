package scalatron.scalatron.api

import akka.actor.ActorSystem
import scalatron.core.{PermanentConfig, Scalatron}


/** This is the "outward" API that Scalatron exposes towards the main function and the web server.
  * It represents the main API entry point of the Scalatron server. It is distinct from the "inward"
  * API that Scalatron exposes towards the game plug-ins it loads.
  */
trait ScalatronOutward extends Scalatron
{
    // ... everything in trait Scalatron, plus:

    /** Starts any background threads required by the Scalatron server (e.g., the compile server). */
    def start()

    /** Passes control of the current thread to a loop that simulates tournament rounds in a
      * Scalatron game server, either until user exits (e.g. by closing the main window) or
      * until the given number of rounds has been played.
      * Note that the run may be headless if the user so requests it in the given argument
      * map ("-headless" -> "yes").
      * @param argMap the command line argument map to use to configure the tournament run
      */
    def run(argMap: Map[String, String] = Map.empty)

    /** Shuts down any background threads created in start(). */
    def shutdown()
}
object ScalatronOutward {
    /** Creates an instance of a Scalatron server and returns a reference to the Scalatron API.
      * Usage:
      * <pre>
      * val scalatron = Scalatron(Map( "-users" -> webUserBaseDirPath, "-plugins" -> pluginBaseDirPath))
      * scalatron.start()
      * scalatron.listUsers()       // or whatever you need
      * scalatron.run()             // run tournament rounds, blocking
      * scalatron.shutdown()
      * </pre>
      */
    def apply(argMap: Map[String, String], actorSystem: ActorSystem, verbose: Boolean = false): ScalatronOutward =
        scalatron.scalatron.impl.ScalatronImpl(argMap, actorSystem, verbose)


    val cmdArgList = Iterable(
        "plugins <dir>  " -> "plug-in base directory (default: ../bots)",
        "samples <dir>  " -> "directory containing example bots (default: ../samples)",
        "rounds <int>   " -> "run this many tournament rounds, then exit (default: unlimited)",
        "headless yes|no" -> "run without visual output (default: no)",
        "verbose yes|no " -> "print verbose output (default: no)"
    ) ++
        PermanentConfig.cmdArgList // TODO: ++ BotWar.cmdArgList

    // undocumented for the moment:
    // println("  -game <name>             the game variant to host (default: BotWar)")
}