/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.core.{Simulation, Plugin}
import Simulation.Time
import util.Random
import java.lang.IllegalStateException


/** A game entity. An entity can be a collidable Bot (player, slave, beast, plant, wall) or
  * a non-colliding decoration (explosion, bonus, bonk, "Say" text).
  */
sealed trait Entity {
    def id: Entity.Id
    def creationTime: Time
    def lifeTime: Time              // expiration time = creationTime + lifeTime
    def pos: XY
}

object Entity {
    type Id = Int
}


/** A bot is a colliding entity, such as a player's master and slave bots, beasts, plants and
  * walls. A bot resides at a position and may have an extent (such as a number of wall cells),
  * in which case the position marks the top-left corner of the covered region.
  */
case class Bot(
    id: Entity.Id,              // unique ID
    pos: XY,                    // (top left) position on board
    extent: XY,                 // number of cells covered in each direction
    creationTime: Time,         // state.time when created
    stunnedUntil: Time,         // stunned until state.time > stunnedUntil
    energy: Int,                // energy budget = score
    variety: Bot.Variety        // variety-specific details (e.g. generation)
    ) extends Entity
{
    require(extent.isNonNegative)

    def lifeTime: Time = Time.MaxValue
    def name = variety.name

    def stunUntil(endTime: Time) = copy(stunnedUntil = endTime)
    def moveTo(newPos: XY) = copy(pos = newPos)
    def updateEnergyBy(energyDelta: Int) = copy(energy = energy + energyDelta)
    def updateEnergyTo(newEnergy: Int) = copy(energy = newEnergy)
    def updateVariety(newVariety: Bot.Variety) = copy(variety = newVariety)

    /** Computes a response for the bot. Returns the input string that was passed to the
      * control function (valid for player bots and mini-bots only) as well as a list of
      * commands issued by the control function. */
    def respondTo(state: State) : (String,Iterable[Command]) = variety.respondTo(state, this)

    def isWall = variety eq Bot.Wall

    def isMaster = variety match {
        case player: Bot.Player => player.isMaster
        case _ => false
    }

    def isSlave = variety match {
        case player: Bot.Player => player.isSlave
        case _ => false
    }

    /** Returns true if this bot is untrusted (plag-in-controlled) and false if this bot is trusted
      * (computer-controlled). Use this to partition the bot collection into trusted/untrusted sub-collections.
      * @return true if this bot is untrusted, false if trusted.
      */
    def isUntrusted = variety.isInstanceOf[Bot.Player]

    def isAt(candidatePos: XY) = {
        candidatePos.x >= pos.x &&
            candidatePos.y >= pos.y &&
            candidatePos.x < pos.x + extent.x &&
            candidatePos.y < pos.y + extent.y
    }
}


object Bot {
    val MasterGeneration = 0        // human-readable constant for the generation of the main player objects

    val OccludedBot = Bot(0, XY.Zero, XY.One, Time.SomtimeInThePast, Time.MaxValue, 0, Occluded)
    val OccludedOpt : Option[Bot] = Some(OccludedBot)




    sealed trait Variety {
        /** Computes a response for the bot. Returns the input string that was passed to the
          * control function (valid for player bots and mini-bots only) as well as a list of
          * commands issued by the control function. */
        def respondTo(state: State, bot: Bot) : (String,Iterable[Command]) = ("",None)
        def name: String
    }


    case class Player(
        controlFunction: (String => String),
        plugin: Plugin,
        generation: Int,
        masterId: Entity.Id,                // for slaves: ID of master that owns them; for masters: their own ID
        rankAndQuartile: (Int,Int),         // for masters: index of the player in the most recent ranking, and the resulting quartile
        cpuTime: Long,                      // nanoseconds of CPU time, accumulated across all cycles
        controlFunctionInput: String,               // for bot debugging: the input string most recently used as input to the control function
        controlFunctionOutput: Iterable[Command],   // for bot debugging: the output string most recently received as output from the control function
        stateMap: Map[String,String]                // state parameter map: key/value pairs settable by the control function
        ) extends Variety
    {
        def isMaster = generation == MasterGeneration
        def isSlave = generation > MasterGeneration

        def name = stateMap.getOrElse(Protocol.PropertyName.Name, "?")

        /** Returns a copy of this bot whose state parameter map was extended or updated with an additional
          * key/value pair.
          * @param extension the key/value pair to add or update in the state parameter map
          * @return a copy of this bot
          */
        def updatedStateMap(extension: (String, String)) = copy(stateMap = stateMap + extension)

        override def respondTo(state: State, bot: Bot) = {
            def computeBotInputForSlave(bot: Bot, state: State) : String = {
                val botPos = bot.pos

                val deltaToMaster = state.board.getBot(masterId) match {
                    case None => XY.Right          // master seems to have disappeared!
                    case Some(masterContainer) =>
                        val masterPos = masterContainer.pos

                        // adjust for torus wrap!
                        val boardSize = state.config.boardParams.size
                        val cellCountX = boardSize.x
                        var dx = masterPos.x - botPos.x                 // e.g. 90 - 10 = 80  or 10 - 90 = -80
                        if(dx > cellCountX/2) dx -= cellCountX          // e.g. if(80>100/2) dx-=100
                        else if(dx < -cellCountX/2) dx += cellCountX    // e.g. if(-80<-100/2) dx+=100

                        val cellCountY = boardSize.y
                        var dy = masterPos.y - botPos.y
                        if(dy > cellCountY/2) dy -= cellCountY
                        else if(dy < -cellCountY/2) dy += cellCountY

                        XY(dx,dy)
                }

                val view = state.flattenedBoard.computeView(botPos, Constants.SlaveHorizonHalfSize)
                computeControlFunctionInput(view, Some(deltaToMaster))
            }

            def computeBotInputForMaster(bot: Bot, state: State): String = {
                val view = state.flattenedBoard.computeView(bot.pos, Constants.MasterHorizonHalfSize)
                computeControlFunctionInput(view, None)
            }
            
            def computeControlFunctionInput(view: FlattenedBoard, deltaToMasterOpt: Option[XY]): String = {
                view.occlude()      // caution: in situ!
                val renderedView = view.cells.map(cell => Player.cellContentToChar(cell, bot) ).mkString
                val stateMapString =
                    stateMap
                    .filter(_._1 != Protocol.PropertyName.Debug)
                    .map(entry => entry._1 + "=" + entry._2)
                    .mkString(",")
                val maybeMasterParameter = 
                    deltaToMasterOpt
                    .map(Protocol.ServerOpcode.ParameterName.Master + "=" + _ + ",")
                    .getOrElse("")
                val controlFunctionInput =
                    Protocol.ServerOpcode.React + "(" +
                        Protocol.PropertyName.Generation + "=" + generation + "," + "" +
                        Protocol.PropertyName.Time + "=" + state.time + "," +
                        Protocol.PropertyName.View + "=" + renderedView + "," +
                        maybeMasterParameter +
                        Protocol.PropertyName.Energy + "=" + bot.energy +
                        (if(stateMapString.isEmpty) "" else "," + stateMapString) +
                        ")"
                        
                controlFunctionInput
            }

            val controlFunctionInput =
                if(isMaster) computeBotInputForMaster(bot, state)
                else computeBotInputForSlave(bot, state)

            val controlFunctionResponse = controlFunction(controlFunctionInput)
            (controlFunctionInput, Command.fromControlFunctionResponse(controlFunctionResponse))
        }
    }
    object Player {
        /** @param targetBotOpt what is being seen
          * @param sourceBot    what is doing the seeing
          */
        def cellContentToChar(targetBotOpt: Option[Bot], sourceBot: Bot) : Char =
            targetBotOpt match {
                case None => '_'
                case Some(targetBot) => targetBot.variety match {
                    case Bot.Occluded => '?'
                    case Bot.GoodPlant => 'P'
                    case Bot.BadPlant => 'p'
                    case Bot.GoodBeast => 'B'
                    case Bot.BadBeast => 'b'
                    case Wall => 'W'
                    case targetPlayer: Bot.Player =>                // target is a player (master or slave)
                        if(targetPlayer.isMaster) {
                            // target is a master
                            sourceBot.variety match {
                                case sourcePlayer: Bot.Player =>    // source is a player (master or slave)
                                    val siblings = sourcePlayer.plugin == targetPlayer.plugin
                                    if(sourcePlayer.isMaster) {
                                        // source is a master, target is a master
                                        if(siblings) 'M' else 'm'
                                    } else {
                                        // source is a slave, target is a master
                                        if(siblings) 'M' else 'm'
                                    }
                                case _ => assert(false); 'X'        // source is not a player (?!?)
                            }
                        } else {
                            // target is a slave
                            sourceBot.variety match {
                                case sourcePlayer: Bot.Player =>    // source is a player (master or slave)
                                    val siblings = sourcePlayer.plugin == targetPlayer.plugin
                                    if(sourcePlayer.isMaster) {
                                        // source is a master, target is a slave
                                        if(siblings) 'S' else 's'
                                    } else {
                                        // source is a slave, target is a slave
                                        if(siblings) 'S' else 's'
                                    }
                                case _ => assert(false); 'X'        // source is not a player (?!?)
                            }
                        }
                    case _ => 'X'
                }
            }
    }




    sealed trait NonPlayer extends Variety { def name: String = getClass.getSimpleName }

    case object Occluded extends NonPlayer
    case object BadPlant extends NonPlayer
    case object GoodPlant extends NonPlayer
    case object Wall extends NonPlayer

    case object BadBeast extends NonPlayer {
        override def respondTo(state: State, bot: Bot) = {
            // move towards nearest player bot
            val command =
                state.board.nearestBotThatIsPlayer(bot.pos).map(nearestPlayerBot => {
                    nearestPlayerBot.variety match {
                        case player: Player =>
                            val BadBeastViewHorizon =
                                if(player.isMaster) {
                                    player.rankAndQuartile._2 match {
                                        case 0 => 80
                                        case 1 => 40
                                        case 2 => 18
                                        case 3 => 6
                                    }
                                } else {
                                    15
                                }
                            val delta = nearestPlayerBot.pos - bot.pos
                            if(delta.length < BadBeastViewHorizon)
                                Command.Move(delta.signum)
                            else
                                Command.Move(XY.randomUnit(new Random))
                        case _ => throw new IllegalStateException
                    }
                })
            ("", command)
        }
    }

    case object GoodBeast extends NonPlayer {
        override def respondTo(state: State, bot: Bot) = {
            // move away from nearest player bot
            val command =
                state.board.nearestBotThatIsPlayer(bot.pos).map(nearestPlayerBot => {
                    nearestPlayerBot.variety match {
                        case player: Player =>
                            val GoodBeastViewHorizon =
                                if(player.isMaster) {
                                    player.rankAndQuartile._2 match {
                                        case 0 => 80
                                        case 1 => 40
                                        case 2 => 18
                                        case 3 => 6
                                    }
                                } else {
                                    15
                                }
                            val delta = nearestPlayerBot.pos - bot.pos
                            if(delta.length < GoodBeastViewHorizon)
                                Command.Move(delta.signum.negate)
                            else
                                Command.Move(XY.randomUnit(new Random))
                        case _ => throw new IllegalStateException
                    }
                })
            ("", command)
        }
    }
}






// trait for ephemeral, non-colliding decorations such as explosion dust, bonus events, etc.
case class Decoration(
    id: Entity.Id,
    pos: XY,
    creationTime: Time,
    variety: Decoration.Variety ) extends Entity
{
    def lifeTime = variety.lifeTime
}


object Decoration {
    sealed trait Variety { def lifeTime: Int }
    case class Explosion(blastRadius: Int) extends Variety { def lifeTime = 20 }
    case class Bonus(energy: Int) extends Variety { def lifeTime = 40 }
    case object Bonk extends Variety { def lifeTime = 40 }
    case object Annihilation extends Variety { def lifeTime = 40 }
    case class Text(text: String) extends Variety { def lifeTime = 40 }
    case class MarkedCell(color: String) extends Variety { def lifeTime = 40 }
    case class Line(toPos: XY, color: String) extends Variety { def lifeTime = 40 }
}










