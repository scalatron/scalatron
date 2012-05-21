/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.util.Random
import scalatron.scalatron.impl.{Plugin, TournamentRoundResult}
import akka.dispatch.ExecutionContext
import akka.actor.ActorSystem


/** Implementations of generic Simulation traits for the BotWar game. */
object BotWarSimulation
{
    case class SimState(gameState: State) extends Simulation.State[SimState,TournamentRoundResult] {
        def step(actorSystem: ActorSystem, executionContextForUntrustedCode: ExecutionContext) = {
            // to make results reproducible, generate a freshly seeded randomizer for every cycle
            val rnd = new Random(gameState.time)

            // apply the game dynamics to the game state
            Dynamics(gameState, rnd, actorSystem, executionContextForUntrustedCode) match {
                case Left(updatedGameState) => Left(SimState(updatedGameState))
                case Right(gameResult) => Right(gameResult)
            }
        }
    }


    case class Factory(config: Config) extends Simulation.Factory[SimState,TournamentRoundResult] {
        def createInitialState(randomSeed: Int, plugins: Iterable[Plugin.External])(executionContextForUntrustedCode: ExecutionContext) = {
            val state = State.createInitial(config, randomSeed, plugins)(executionContextForUntrustedCode)
            SimState(state)
        }
    }
}