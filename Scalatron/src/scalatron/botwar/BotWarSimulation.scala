/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.util.Random
import scalatron.scalatron.impl.{Plugin, TournamentRoundResult}
import akka.actor.ActorSystem


/** Implementations of generic Simulation traits for the BotWar game. */
object BotWarSimulation
{
    case class SimState(gameState: State) extends Simulation.State[SimState,TournamentRoundResult] {
        def step(implicit actorSystem: ActorSystem) = {
            // to make results reproducible, generate a freshly seeded randomizer for every cycle
            val rnd = new Random(gameState.time)

            // apply the game dynamics to the game state
            Dynamics(gameState, rnd, actorSystem) match {
                case Left(updatedGameState) => Left(SimState(updatedGameState))
                case Right(gameResult) => Right(gameResult)
            }
        }
    }


    case class Factory(config: Config) extends Simulation.Factory[SimState,TournamentRoundResult] {
        def createInitialState(randomSeed: Int, plugins: Iterable[Plugin.External]) = {
            // inject internally implemented players into the plug-in list
            val combinedPlugins = config.permanent.internalPlugins ++ plugins

            val state = State.createInitial(config, randomSeed, combinedPlugins)
            SimState(state)
        }
    }
}