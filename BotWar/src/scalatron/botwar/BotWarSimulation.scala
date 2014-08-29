/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.concurrent.ExecutionContext
import scala.util.Random
import akka.actor.ActorSystem
import scalatron.core.{EntityController, Simulation}


/** Implementations of generic Simulation traits for the BotWar game. */
object BotWarSimulation
{
    case class SimState(gameState: State) extends Simulation.State[SimState] {
        def time = gameState.time
        def step(actorSystem: ActorSystem, executionContextForUntrustedCode: ExecutionContext) = {
            // to make results reproducible, generate a freshly seeded randomizer for every cycle
            val rnd = new Random(gameState.time)

            // apply the game dynamics to the game state
            Dynamics(gameState, rnd, actorSystem, executionContextForUntrustedCode) match {
                case Left(updatedGameState) => Left(SimState(updatedGameState))
                case Right(gameResult) => Right(gameResult)
            }
        }
        /** Returns a collection containing all entities controlled by the control function implemented in the plug-in
          * (and thus associated with the player) with the given name. */
        def entitiesOfPlayer(name: String) =
            gameState.board.entitiesOfPlayer(name).map(e => new Simulation.Entity {
                def id = e.id
                def name = e.name
                def isMaster = e.isMaster
                def mostRecentControlFunctionInput = e.variety match {
                    case player: Bot.Player => player.controlFunctionInput
                    case _ => ""
                }
                def mostRecentControlFunctionOutput = e.variety match {
                    case player: Bot.Player =>
                        val commands = player.controlFunctionOutput
                        commands.map(command => (command.opcode, command.paramMap.map(e => (e._1, e._2.toString))))
                    case _ => Iterable.empty
                }
                def debugOutput = e.variety match {
                    case player: Bot.Player => player.stateMap.getOrElse(Protocol.PropertyName.Debug, "")
                    case _ => ""
                }
            })
    }


    case class Factory(config: Config) extends Simulation.Factory[SimState] {
        def createInitialState(randomSeed: Int, entityControllers: Iterable[EntityController], executionContextForUntrustedCode: ExecutionContext) = {
            val state = State.createInitial(config, randomSeed, entityControllers, executionContextForUntrustedCode)
            SimState(state)
        }
    }
}