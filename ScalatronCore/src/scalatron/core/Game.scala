/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

package scalatron.core

import scala.concurrent.ExecutionContext


/** This is the API that game plug-ins implement and expose toward the managing Scalatron server. */
trait Game
{
    /** @return the package path to be used for constructing fully qualified class names for bot plug-ins,
      *         e.g. "scalatron.botwar.botPlugin" */
    def gameSpecificPackagePath: String

    /** Returns a collection of game-specific command line configuration options (key, value pairs). */
    def cmdArgList : Iterable[(String,String)]

    /** Run a loop that simulates tournament rounds, either until user exists (e.g. by closing
      * the main window) or until the given number of rounds has been played.
      * The callback methods of the Scalatron core will be invoked at the appropriate times.
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param scalatron reference to the Scalatron core, the container managing the game plug-in
      */
    def runVisually(rounds: Int, scalatron: ScalatronInward)


    /** Run a loop that simulates tournament rounds until the given number of rounds has been
      * played. Does not open a display window.
      * The callback methods of the Scalatron core will be invoked at the appropriate times.
      * @param rounds the number of rounds to play in the tournament loop before returning
      * @param scalatron reference to the Scalatron core, the container managing the game plug-in
      */
    def runHeadless(rounds: Int, scalatron: ScalatronInward)


    /** Starts a headless, private game instance and returns the associated initial simulation state.
      * This facility is used when a sandboxed game instance is requested for bot debugging.
      * @param entityControllers the plug-in-based entity controller(s) to load into the game
      * @param roundConfig the configuration for this game round (includes the permanent configuration by reference)
      * @param executionContextForUntrustedCode execution context for untrusted code (e.g. for bot control functions)
      * @return an initial simulation state.
      */
    def startHeadless(entityControllers: Iterable[EntityController], roundConfig: RoundConfig, executionContextForUntrustedCode: ExecutionContext): Simulation.UntypedState
}
