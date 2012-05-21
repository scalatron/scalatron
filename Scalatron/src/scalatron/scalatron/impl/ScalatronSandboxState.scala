package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.core.Scalatron
import scalatron.core.Scalatron.SandboxState
import scalatron.core.Simulation


case class ScalatronSandboxState(sandbox: ScalatronSandbox, simState: Simulation.UntypedState) extends Scalatron.SandboxState {
    def time = simState.time.toInt      // CBB: warn on truncation from Long to Int

    def step(count: Int): SandboxState = {
        val actorSystem = sandbox.user.scalatron.actorSystem
        val executionContextForUntrustedCode = sandbox.user.scalatron.executionContextForUntrustedCode
        var updatedState = simState
        for( i <- 0 until count ) {
            updatedState.step(actorSystem, executionContextForUntrustedCode) match {
                case Left(successorState) =>
                    updatedState = successorState

                case Right(result) =>
                    // the simulator should have been configured to run forever
                    throw new IllegalStateException("expected never-ending simulation")
            }
        }
        copy(simState = updatedState)
    }

    def entities = simState.entitiesOfPlayer(sandbox.user.name).map(e => ScalatronSandboxEntity(e, this))
}