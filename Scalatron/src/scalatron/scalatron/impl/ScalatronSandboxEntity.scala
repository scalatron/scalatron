package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.scalatron.api.Scalatron
import scalatron.botwar.{Protocol, Bot}


/** Implements a wrapper for an entity in a Scalatron sandbox.
  */
case class ScalatronSandboxEntity(bot: Bot, state: ScalatronSandboxState) extends Scalatron.SandboxEntity
{
    def id = bot.id
    def name = bot.name
    def isMaster = bot.isMaster

    def mostRecentControlFunctionInput = bot.variety match {
        case player: Bot.Player => player.controlFunctionInput
        case _ => ""
    }

    def mostRecentControlFunctionOutput: Iterable[(String, Iterable[(String, String)])] = bot.variety match {
        case player: Bot.Player =>
            val commands = player.controlFunctionOutput
            commands.map(command => (command.opcode, command.paramMap.map(e => (e._1, e._2.toString))))
        case _ => Iterable.empty
    }

    def debugOutput = bot.variety match {
        case player: Bot.Player => player.stateMap.getOrElse(Protocol.PropertyName.Debug, "")
        case _ => ""
    }
}