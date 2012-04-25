package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.botwar.BotWarSimulation
import scalatron.scalatron.api.Scalatron


case class ScalatronSandbox(id: Int, user: ScalatronUser, initialSimState: BotWarSimulation.SimState) extends Scalatron.Sandbox {
    def initialState = ScalatronSandboxState(this, initialSimState)
}
