/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.concurrent.ExecutionContext
import scalatron.core.EntityController
import scalatron.core.Simulation.Time


/** Game state storing the current (game) time, the board parameters, the actual board (i.e.
  * where is each entity) as well as a player ranking and configuration settings for the game.
  */
case class State(
    time: Time,
    board: Board,
    rankedPlayers: Array[Bot],
    config: Config)
{
    /** A cache containing the "flattened" board, i.e. a cell-by-cell rendering of the board
      * that is used to compute (occluded or unoccluded) views for all bots.
      */
    lazy val flattenedBoard = board.flatten(config.boardParams.size)

    /** Wraps a given coordinate such that it toroidally wraps back into the arena. */
    def wrap(pos: XY) = pos.wrap(config.boardParams.size)
}


object State
{
    def createInitial(config: Config, randomSeed: Int, entityControllers: Iterable[EntityController], executionContextForUntrustedCode: ExecutionContext) = {
        val time = 0L
        val board =
            Board.createInitial(
                config.boardParams,
                time,
                config.permanent.stepsPerRound,
                config.roundIndex,
                randomSeed,
                entityControllers
            )(
                executionContextForUntrustedCode
            )
        State(time, board, Array.empty, config)
    }
}

