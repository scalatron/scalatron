package scalatron.scalatron.impl

import akka.dispatch.ExecutionContext

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
/** Base trait for game implementations that run within a Scalatron server (e.g. BotWar). */
trait Game {
    def name: String // e.g. "BotWar"

    /** The parameters of this game that are relevant for loading plug-ins (e.g. game name). */
    def pluginLoadSpec: PluginCollection.LoadSpec

    /** Dump the game-specific command line configuration options via println. */
    def printArgList()

    /** Run a loop that simulates tournament rounds, either until user exists (e.g. by closing
      * the main window) or until the given number of rounds has been played.
      * @param pluginPath the plug-in base directory path below which to scan for bot plug-ins
      * @param argMap the command line argument map to use to configure the tournament run
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param tournamentState the tournament state object to update whenever a round ends
      * @param secureMode if true, certain bot processing restrictions apply
      * @param verbose if true, log to the console verbosely
      * @param executionContextForTrustedCode execution context for trusted code (e.g. from Akka ActorSystem)
      * @param executionContextForUntrustedCode execution context for untrusted code (e.g. for bot control functions)
      */
    def runVisually(
        pluginPath: String,
        argMap: Map[String, String],
        rounds: Int,
        tournamentState: TournamentState,
        secureMode: Boolean,
        verbose: Boolean
    )(
        executionContextForTrustedCode: ExecutionContext,
        executionContextForUntrustedCode: ExecutionContext
    )


    /** Run a loop that simulates tournament rounds until the given number of rounds has been
      * played. Does not open a display window.
      * @param pluginPath the plug-in base directory path below which to scan for bot plug-ins
      * @param argMap the command line argument map to use to configure the tournament run
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param tournamentState the tournament state object to update whenever a round ends
      * @param secureMode if true, certain bot processing restrictions apply
      * @param verbose if true, log to the console verbosely
      * @param executionContextForTrustedCode execution context for trusted code (e.g. from Akka ActorSystem)
      * @param executionContextForUntrustedCode execution context for untrusted code (e.g. for bot control functions)
      */
    def runHeadless(
        pluginPath: String,
        argMap: Map[String, String],
        rounds: Int,
        tournamentState: TournamentState,
        secureMode: Boolean,
        verbose: Boolean
    )(
        executionContextForTrustedCode: ExecutionContext,
        executionContextForUntrustedCode: ExecutionContext
    )

}
