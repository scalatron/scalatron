package scalatron.scalatron.impl

import akka.actor.ActorSystem

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
      * @param actorSystem the Scalatron Akka actor system; use this for concurrency
      * @param pluginPath the plug-in base directory path below which to scan for bot plug-ins
      * @param argMap the command line argument map to use to configure the tournament run
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param tournamentState the tournament state object to update whenever a round ends
      * @param verbose if true, log to the console verbosely
      */
    def runVisually(
        pluginPath: String,
        argMap: Map[String, String],
        rounds: Int,
        tournamentState: TournamentState,
        verbose: Boolean)
            (implicit actorSystem: ActorSystem)


    /** Run a loop that simulates tournament rounds until the given number of rounds has been
      * played. Does not open a display window.
      * @param actorSystem the Scalatron Akka actor system; use this for concurrency
      * @param pluginPath the plug-in base directory path below which to scan for bot plug-ins
      * @param argMap the command line argument map to use to configure the tournament run
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param tournamentState the tournament state object to update whenever a round ends
      * @param verbose if true, log to the console verbosely
      */
    def runHeadless(
        pluginPath: String,
        argMap: Map[String, String],
        rounds: Int,
        tournamentState: TournamentState,
        verbose: Boolean)
            (implicit actorSystem: ActorSystem)

}
