package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.core.Scalatron
import scalatron.core.Simulation


/** Implements a wrapper for an entity in a Scalatron sandbox.
  */
case class ScalatronSandboxEntity(entity: Simulation.Entity, state: ScalatronSandboxState) extends Scalatron.SandboxEntity
{
    def id = entity.id
    def name = entity.name
    def isMaster = entity.isMaster
    def mostRecentControlFunctionInput = entity.mostRecentControlFunctionInput
    def mostRecentControlFunctionOutput = entity.mostRecentControlFunctionOutput
    def debugOutput = entity.debugOutput
}