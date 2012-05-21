package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.core.Scalatron
import scalatron.core.Simulation


case class ScalatronSandbox(id: Int, user: ScalatronUser, initialSimState: Simulation.UntypedState) extends Scalatron.Sandbox {
    def initialState = ScalatronSandboxState(this, initialSimState)
}
