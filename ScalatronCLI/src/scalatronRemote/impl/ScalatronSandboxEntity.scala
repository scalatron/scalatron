package scalatronRemote.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
import scalatronRemote.api.ScalatronRemote
import scalatronRemote.api.ScalatronRemote.Command

case class ScalatronSandboxEntity(
    id: Int,
    name: String,
    isMaster: Boolean,
    mostRecentControlFunctionInput: Command,
    mostRecentControlFunctionOutput: Iterable[Command],
    debugOutput: String
) extends ScalatronRemote.SandboxEntity {
  override def toString = id + " -> " + name
}
