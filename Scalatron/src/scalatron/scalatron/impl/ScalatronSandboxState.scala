package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.botwar.BotWarSimulation
import scalatron.scalatron.api.Scalatron
import scalatron.scalatron.api.Scalatron.SandboxState


case class ScalatronSandboxState(sandbox: ScalatronSandbox, simState: BotWarSimulation.SimState) extends Scalatron.SandboxState {
    def time = simState.gameState.time.toInt      // CBB: warn on truncation from Long to Int

    def step(count: Int): SandboxState = {
        implicit val actorSystem = sandbox.user.scalatron.actorSystem
        var updatedState = simState
        for( i <- 0 until count ) {
            updatedState.step match {
                case Left(successorState) =>
                    updatedState = successorState

                case Right(result) =>
                    // the simulator should have been configured to run forever
                    throw new IllegalStateException("expected never-ending simulation")
            }
        }
        copy(simState = updatedState)
    }

    def entities = {
        val gameState = simState.gameState
        val board = gameState.board
        val entities = board.entitiesOfPlayer(sandbox.user.name)
        entities.map(e => ScalatronSandboxEntity(e, this))
    }
}