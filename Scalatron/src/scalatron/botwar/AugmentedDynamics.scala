/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.util.Random
import scalatron.botwar.Decoration.{Bonus, Explosion, Text, Bonk, Annihilation}
import scalatron.scalatron.impl.TournamentRoundResult


case object AugmentedDynamics extends ((State,Random,Iterable[(Entity.Id,Iterable[Command])]) => Either[State,TournamentRoundResult])
{
    def apply(state: State, rnd: Random, commands: Iterable[(Entity.Id,Iterable[Command])]): Either[State,TournamentRoundResult] =
    {
        val updatedTime = state.time + 1
        if( updatedTime > state.config.permanent.stepsPerRound ) {
            val masterBots = state.board.botsThatAreMasters
            val namesAndScores = masterBots.map(bot => (bot.name, bot.energy)).toMap

            // inform the plug-ins that the game is over
            masterBots.foreach(bot => try {
                bot.variety match {
                    case player: Bot.Player => player.controlFunction(Protocol.ServerOpcode.Goodbye + "(energy=" + bot.energy + ")")
                    case _ => throw new IllegalStateException("expected master bot")
                }
            } catch {
                case t: Throwable => System.err.println("Bot '" + bot.name + "' caused an error in 'Goodbye': " + t)
            })

            return Right(TournamentRoundResult(namesAndScores))
        }

        // this variable tracks the updated state while we process player commands and move stuff around
        var updatedBoard = state.board


        // process bot commands
        commands.foreach(idAndCommand => {
            val botId = idAndCommand._1
            idAndCommand._2.foreach(command => {
                updatedBoard.getBot(botId) match {
                    case None => // entity with this ID was deleted in the meantime
                    case Some(bot) => updatedBoard = processCommand(bot, command, state, updatedBoard)
                }
            })
        })


        // process slave bot energy depletion
        if( (state.time % Constants.Energy.SlaveDepletionCycleSpacing) == 0) {
            updatedBoard.botsThatAreSlavesOrBeasts.foreach(bot => {
                val updatedEnergy = bot.energy - Constants.Energy.SlaveDepletionPerSpacedCycle
                if(updatedEnergy <= 0) {
                    updatedBoard = updatedBoard.removeBot(bot.id)
                    updatedBoard = updatedBoard.addDecoration(bot.pos, state.time, Annihilation)
                } else {
                    val updatedBot = bot.updateEnergyBy( -Constants.Energy.SlaveDepletionPerSpacedCycle )
                    updatedBoard = updatedBoard.updateBot(updatedBot)
                }
            } )
        }

        // eliminate expired bots and decorations
        // updatedBoard = updatedBoard.copy(bots = updatedBoard.bots.filter(entry => (entry._2.creationTime + entry._2.lifeTime) <= state.time) )
        updatedBoard = updatedBoard.copy(decorations = updatedBoard.decorations.filter(entry => (entry._2.creationTime + entry._2.lifeTime > state.time)))



        // replenish plants & beasts
        val boardParams = state.config.boardParams
        val boardSize = boardParams.size
        val goodPlantDeficit = boardParams.goodPlantCount - updatedBoard.botsThatAreGoodPlants.size
        if(goodPlantDeficit > 0) updatedBoard = updatedBoard.sprinkleGoodPlants(goodPlantDeficit, rnd, state.time, boardSize)
        val badPlantDeficit = boardParams.badPlantCount - updatedBoard.botsThatAreBadPlants.size
        if(badPlantDeficit > 0) updatedBoard = updatedBoard.sprinkleBadPlants(badPlantDeficit, rnd, state.time, boardSize)
        val goodBeastDeficit = boardParams.goodBeastCount - updatedBoard.botsThatAreGoodBeasts.size
        if(goodBeastDeficit > 0) updatedBoard = updatedBoard.sprinkleGoodBeasts(goodBeastDeficit, rnd, state.time, boardSize)
        val badBeastDeficit = boardParams.badBeastCount - updatedBoard.botsThatAreBadBeasts.size
        if(badBeastDeficit > 0) updatedBoard = updatedBoard.sprinkleBadBeasts(badBeastDeficit, rnd, state.time, boardSize)

        // compute the player ranking and mark up the player bots
        val players = updatedBoard.botsThatAreMasters.toArray
        val rankedPlayers = players.sortBy(-_.energy)
        rankedPlayers.zipWithIndex.foreach(botAndIndex => {
            val bot = botAndIndex._1
            val rank = botAndIndex._2
            val playerCount = rankedPlayers.length
            val quartile = if(playerCount<4) 2 else 3 * (rank+1) / playerCount  // 1 of 10 => 0, 10 of 10 => 3
            val updatedVariety = bot.variety match {
                case player: Bot.Player => player.copy(rankAndQuartile = (rank,quartile))
                case _ => bot.variety
            }
            updatedBoard = updatedBoard.updateBot(bot.updateVariety(updatedVariety))
        })

        Left(state.copy(time = updatedTime, board = updatedBoard, rankedPlayers = rankedPlayers))
    }


    def processCommand(thisBot: Bot, command: Command, state: State, board: Board) : Board = {
        val time = state.time
        val thisVariety = thisBot.variety

        try {
            val thisBotId = thisBot.id
            val thisBotPos = thisBot.pos
            var updatedBoard = board

            command match {
                case Command.Nop =>                   // "Nop()"
                    // nothing to do

                case move: Command.Move =>            // "Move(dx=<int>,dy=<int>)"
                    val delta = move.offset.signum
                    if( delta.isNonZero ) {
                        val proposedPos = state.wrap( thisBotPos + delta )
                        board.occupant(proposedPos) match {
                            case None =>
                                // vacant -- walk there
                                updatedBoard = updatedBoard.updateBot(thisBot.moveTo(proposedPos))

                            case Some(otherBot) =>
                                // not vacant -- collision
                                updatedBoard = processCollision(thisBot, proposedPos, otherBot, state, updatedBoard)
                        }
                    }

                case spawn: Command.Spawn =>           // "Spawn(dx=<int>,dy=<int>,name=<int>,energy=<int>)"
                    thisVariety match {
                        case thisPlayer: Bot.Player =>
                            val energy = spawn.map.get(Protocol.PropertyName.Energy).map(_.toInt).getOrElse( 100 )
                            if( energy < 100 ) throw new IllegalStateException("Spawn(): requested energy less than minimum (100): " + energy)
                            val updatedBotEnergy = thisBot.energy - energy
                            if( updatedBotEnergy < 0 ) throw new IllegalStateException("Spawn(): bot does not have enough energy: " + energy + " vs " + thisBot.energy )

                            val maxSlaveCount = state.config.boardParams.maxSlaveCount
                            if(maxSlaveCount < Int.MaxValue) {
                                val siblings = board.siblingsOfBot(thisBot)
                                val siblingCount = siblings.size
                                if(siblingCount > maxSlaveCount + 1) {
                                    throw new IllegalStateException("Spawn(): maximum permissible number of mini-bots has been reached: " + maxSlaveCount )
                                }
                            }

                            val direction = spawn.map.get(Protocol.PluginOpcode.ParameterName.Direction).map(s => XY(s)).getOrElse(XY.One)
                            val delta = direction.signum
                            val spawnedPos = state.wrap( thisBotPos + delta )
                            if( board.isVacant(spawnedPos) ) {
                                // update spawner
                                val updatedThisBot = thisBot.updateEnergyTo(updatedBotEnergy)
                                updatedBoard = updatedBoard.updateBot(updatedThisBot)

                                // allow master to set slave's initial state
                                var slaveStateMap = Map.empty[String,String]
                                spawn.map.foreach(entry => {
                                    // prevent bot from setting reserved keywords
                                    val key = entry._1
                                    val value = entry._2
                                    if(!Protocol.PropertyName.isReserved(key) && !value.isEmpty) {
                                        // we skip reserved keys silently, since it may be valid outside this loop
                                        slaveStateMap = slaveStateMap.updated(key, value)
                                    }
                                })

                                val slaveName = spawn.map.get("name").getOrElse("Slave_" + time)
                                slaveStateMap += Protocol.PropertyName.Name -> slaveName

                                // update spawnee
                                val slaveVariety = thisPlayer.copy(generation = thisPlayer.generation + 1, stateMap = slaveStateMap)
                                updatedBoard = updatedBoard.addBot(spawnedPos, XY.One, time, energy, slaveVariety)
                            }
                        case _ => // not a permissible command!
                            throw new IllegalStateException("Spawn(): not permitted for bot variety: " + thisVariety)
                    }

                case status: Command.Status =>           // "Status(text=<string>)"
                    thisVariety match {
                        case thisPlayer: Bot.Player =>
                            val text = status.text
                            var updatedStateMap = thisPlayer.stateMap.updated(Protocol.PropertyName.Status, text)
                            val updatedVariety = thisPlayer.copy(stateMap = updatedStateMap)
                            updatedBoard = updatedBoard.updateBot(thisBot.updateVariety(updatedVariety))
                        case _ =>
                            throw new IllegalStateException("Status(): not permitted for bot variety: " + thisVariety)
                    }

                case say: Command.Say =>                // "Say(text=<string>)"
                    val text = say.text
                    val clippedText = if(text.length<=20) text else text.substring(0,10)
                    updatedBoard = updatedBoard.addDecoration(thisBotPos, time, Text(clippedText))


                case set: Command.Set =>                // "Set(key=value,key=value)"
                    thisVariety match {
                        case thisPlayer: Bot.Player =>
                            var updatedStateMap = thisPlayer.stateMap
                            set.map.foreach(entry => {
                                // prevent bot from setting reserved keywords
                                val key = entry._1
                                if(Protocol.PropertyName.isReserved(key)) {
                                    throw new IllegalStateException("Set() with illegal property key name: " + key)
                                }
                                val value = entry._2
                                updatedStateMap =
                                    if(value.isEmpty)
                                        updatedStateMap - key       // empty value => delete the key
                                    else
                                        updatedStateMap.updated(key, value)
                            })
                            val updatedVariety = thisPlayer.copy(stateMap = updatedStateMap)
                            val updatedThisBot = thisBot.updateVariety(updatedVariety)
                            updatedBoard = updatedBoard.updateBot(updatedThisBot)
                        case _ => // not a permissible command!
                            throw new IllegalStateException("Set(): not permitted for bot variety: " + thisVariety)
                    }

                case log: Command.Log =>                // "Log(text=<string>)"
                    thisVariety match {
                        case thisPlayer: Bot.Player =>
                            val currentDebugOutput = thisPlayer.stateMap.getOrElse(Protocol.PropertyName.Debug, "")
                            val updatedDebugOutput =
                                currentDebugOutput +
                                    (if(currentDebugOutput.isEmpty) "" else "\n") +
                                    log.text
                            val updatedStateMap = thisPlayer.stateMap.updated(Protocol.PropertyName.Debug, updatedDebugOutput)
                            val updatedVariety = thisPlayer.copy(stateMap = updatedStateMap)
                            val updatedThisBot = thisBot.updateVariety(updatedVariety)
                            updatedBoard = updatedBoard.updateBot(updatedThisBot)
                        case _ => // not a permissible command!
                            throw new IllegalStateException("Log(): not permitted for bot variety: " + thisVariety)
                    }

                case disable: Command.Disable =>                // "Disable(text=<string>)"
                    thisVariety match {
                        case thisPlayer: Bot.Player =>
                            val logMessage = "plug-in was disabled because it caused an error"
                            val updatedStateMap =
                                thisPlayer.stateMap
                                .updated(Protocol.PropertyName.Status, "disabled")
                                .updated(Protocol.PropertyName.Debug, logMessage + "\n" + disable.text)
                            val updatedVariety =
                                thisPlayer.copy(
                                controlFunction = (in: String) => "Log(text=" + logMessage + ")", // make sure it's never called again (but note that sibling minibots/bots use their own ref)
                                stateMap = updatedStateMap)
                            val updatedThisBot = thisBot.updateVariety(updatedVariety)
                            updatedBoard = updatedBoard.updateBot(updatedThisBot)
                        case _ => // not a permissible command!
                            throw new IllegalStateException("Disable(): not permitted for bot variety: " + thisVariety)
                    }

                case explode: Command.Explode =>         // "Explode(size=<int>)"
                    thisBot.variety match {
                        case thisPlayer: Bot.Player =>  // master or slave
                            if(thisPlayer.isMaster) {
                                throw new IllegalStateException("Explode() command is illegal for masters")
                            } else {
                                // a slave is exploding -- remove it, deal out damage, bonus for master
                                updatedBoard = updatedBoard.removeBot(thisBotId)

                                // compute damage
                                val blastRadius =
                                    if(explode.blastRadius < Constants.MinBlastRadius) Constants.MinBlastRadius
                                    else if(explode.blastRadius > Constants.MaxBlastRadius) Constants.MaxBlastRadius
                                    else explode.blastRadius

                                val energy = thisBot.energy
                                val blastArea = blastRadius * blastRadius * math.Pi
                                val energyPerArea = energy / blastArea
                                val damageAtCenter = - Constants.Energy.ExplosionDamageFactor * energyPerArea // negative!
                                def damage(distance: Double) : Int = {
                                    val distanceFactor = (1 - (distance / blastRadius))
                                    (damageAtCenter * distanceFactor).intValue
                                }

                                updatedBoard = updatedBoard.addDecoration(thisBotPos, time, Explosion(blastRadius))

                                val thisSlavesMasterId = thisPlayer.masterId
                                val affectedBots = updatedBoard.botsNear(thisBotPos, blastRadius)
                                var totalDamageForAffected = 0              // will be negative
                                affectedBots.foreach(affectedBot => affectedBot.variety match {
                                    case affectedPlayer: Bot.Player =>      // master or slave
                                        if(affectedPlayer.masterId == thisSlavesMasterId) {
                                            // a sibling => no damage!
                                        } else {
                                            // an enemy => damage!
                                            val affectedBotPos = affectedBot.pos
                                            val distanceFromCenter = affectedBotPos.distanceTo(thisBotPos)
                                            val explosionDamage = damage(distanceFromCenter)         // negative

                                            // do not allow for more damage than opponent has energy
                                            // this devalues repeated attacks on passive (non-moving) players
                                            val actualDamage =
                                                if(affectedBot.energy <=0) 0
                                                else explosionDamage.max(-affectedBot.energy)

                                            val updatedAffectedBot = affectedBot.updateEnergyBy(actualDamage)
                                            updatedBoard = updatedBoard.updateBot(updatedAffectedBot)
                                            updatedBoard = updatedBoard.addDecoration(affectedBotPos, time, Bonus(actualDamage))

                                            totalDamageForAffected += actualDamage
                                        }

                                    case Bot.BadBeast =>      // master or slave
                                        // an enemy => damage!
                                        val affectedBotPos = affectedBot.pos
                                        val distanceFromCenter = affectedBotPos.distanceTo(thisBotPos)
                                        val explosionDamage = damage(distanceFromCenter)    // negative
                                        val actualDamage = explosionDamage.max(-200)        // prevent extensive damage (thanks for the fix, Ian Calvert!)

                                        val updatedAffectedBot = affectedBot.updateEnergyBy(actualDamage)
                                        updatedBoard = updatedBoard.updateBot(updatedAffectedBot)
                                        updatedBoard = updatedBoard.addDecoration(affectedBotPos, time, Bonus(actualDamage))

                                        totalDamageForAffected += actualDamage

                                    case _ =>                   // wall, plant, beast
                                } )

                                // find the master
                                updatedBoard.getBot(thisSlavesMasterId) match {
                                    case None => assert(false)
                                    case Some(thisSlavesMaster) =>
                                        val energyDelta = -totalDamageForAffected
                                        updatedBoard = updatedBoard.updateBot(thisSlavesMaster.updateEnergyBy(energyDelta))
                                        updatedBoard = updatedBoard.addDecoration(thisSlavesMaster.pos, time, Bonus(energyDelta))
                                }
                            }
                        case _ =>
                            throw new IllegalStateException("Explode() command is illegal for non-player bots")
                    }

                case markedCell: Command.MarkCell =>           // "MarkCell(position=x:y,color=#ff0000)"
                    updatedBoard = updatedBoard.addDecoration(thisBotPos + markedCell.pos, time, Decoration.MarkedCell(markedCell.color))

                case line: Command.DrawLine =>                 // "DrawLine(from=x1:y1,to=x2:y2,color=#cccccc)"
                    updatedBoard = updatedBoard.addDecoration(
                        thisBotPos + line.fromPos, time, Decoration.Line(thisBotPos + line.toPos, line.color))

                case _ =>
                    // unknown command!
                    throw new IllegalStateException("unknown command: " + command)
            }

            updatedBoard
        } catch {
            case t: Throwable =>
                // System.err.println("Entity '" + thisBot.name + "' caused an error: " + t)

                // store the error on the bot
                thisVariety match {
                    case thisPlayer: Bot.Player =>
                        val currentDebugOutput = thisPlayer.stateMap.getOrElse(Protocol.PropertyName.Debug, "")
                        val updatedDebugOutput =
                            currentDebugOutput +
                                (if(currentDebugOutput.isEmpty) "" else "\n") +
                                "this entity caused an error:\n" + t
                        val updatedStateMap = thisPlayer.stateMap.updated(Protocol.PropertyName.Debug, updatedDebugOutput)
                        val updatedVariety = thisPlayer.copy(stateMap = updatedStateMap)
                        val updatedThisBot = thisBot.updateVariety(updatedVariety)
                        board.updateBot(updatedThisBot)
                    case _ => board
                }
        }
    }


    /** This method is invoked to determine the effects of a bot attempting to step on a cell that is already
      * occupied by another entity. The invocation can be understood by thinking "the moving bot desires to replace
      * its currently still active precursor version", and this method computes what should happen.
      * @param movingBot the bot that desires to move to a new cell
      * @param proposedPos the position of the cell to which a move is proposed
      * @param steppedOnBot the bot that currently occupies the cell to which a move is proposed
      * @param state the current game state (for access to time and configuration)
      * @param board the current board state (which is updated and returned)
      * @return the updated board state
      */
    def processCollision(movingBot: Bot, proposedPos: XY, steppedOnBot: Bot, state: State, board: Board): Board = {
        val movingBotPos = movingBot.pos
        val time = state.time
        var updatedBoard = board
        movingBot.variety match {
            case movingPlayer: Bot.Player =>

                /** Update an entity's state map */
                def updateStateMap(extension: (String, String)) { updatedBoard = updatedBoard.updateBot(movingBot.updateVariety(movingPlayer.updatedStateMap(extension))) }

                /** Record the collision in the entity's state map */
                def bonk() { updateStateMap(Protocol.PropertyName.Collision, (proposedPos - movingBotPos).toString) }

                steppedOnBot.variety match {
                    case steppedOnPlayer: Bot.Player =>      // player on player -- depends...
                        if(movingPlayer.isMaster) {
                            // master on...
                            if(steppedOnPlayer.isMaster) {
                                // master on master -- bonk
                                updatedBoard = updatedBoard.addDecoration(movingBotPos, state.time, Bonk)
                                bonk()
                            } else {
                                // master on slave
                                if(movingPlayer.plugin == steppedOnPlayer.plugin) {
                                    // master on own slave -- re-absorb
                                    updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                                    val energyDelta = steppedOnBot.energy
                                    updatedBoard = updatedBoard.updateBot(movingBot.moveTo(proposedPos).updateEnergyBy(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, state.time, Annihilation)
                                } else {
                                    // master on enemy slave -- slave is eaten
                                    updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                                    val energyDelta = Constants.Energy.ValueForMasterFromEatingEnemySlave
                                    updatedBoard = updatedBoard.updateBot(movingBot.moveTo(proposedPos).updateEnergyBy(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))
                                }
                            }
                        } else {
                            if(steppedOnPlayer.isMaster) {
                                // slave on master -- always disappears
                                if(movingPlayer.plugin == steppedOnPlayer.plugin) {
                                    // slave on own master -- gets re-absorbed
                                    updatedBoard = updatedBoard.removeBot(movingBot.id)
                                    val energyDelta = movingBot.energy
                                    updatedBoard = updatedBoard.updateBot(steppedOnBot.updateEnergyBy(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, state.time, Annihilation)
                                } else {
                                    // slave on enemy master -- slave gets eaten
                                    updatedBoard = updatedBoard.removeBot(movingBot.id)
                                    val energyDelta = Constants.Energy.ValueForMasterFromEatingEnemySlave
                                    updatedBoard = updatedBoard.updateBot(steppedOnBot.updateEnergyBy(energyDelta))
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))
                                }
                            } else {
                                // slave on slave -- bonk or annihilation
                                if(movingPlayer.masterId == steppedOnPlayer.masterId) {
                                    // slave on sibling slave -- bonk
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonk)
                                    bonk()
                                } else {
                                    // slave on enemy slave -- annihilation
                                    updatedBoard = updatedBoard.removeBot(movingBot.id)
                                    updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                                    updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Annihilation)
                                }
                            }
                        }

                    case Bot.GoodBeast =>   // player on good beast -- beast gets eaten
                        updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                        val energyDelta = Constants.Energy.ValueForPlayerFromEatingGoodBeast
                        updatedBoard = updatedBoard.updateBot(movingBot.moveTo(proposedPos).updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))

                    case Bot.BadBeast =>    // player on bad beast -- both get pain
                        val energyDelta = Constants.Energy.PainForBoth_Player_vs_BadBeast
                        updatedBoard = updatedBoard.updateBot(movingBot.updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.updateBot(steppedOnBot.updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))   // signal for player
                        bonk()

                    case Bot.GoodPlant =>   // player on good plant -- plant gets eaten
                        updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                        val energyDelta = Constants.Energy.ValueForPlayerFromEatingGoodPlant
                        updatedBoard = updatedBoard.updateBot(movingBot.moveTo(proposedPos).updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))

                    case Bot.BadPlant =>    // player on bad plant -- plant disappears, player gets pain
                        updatedBoard = updatedBoard.removeBot(steppedOnBot.id)
                        val energyDelta = Constants.Energy.PainForPlayerFromSteppingOnBadPlant
                        updatedBoard = updatedBoard.updateBot(movingBot.moveTo(proposedPos).updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))

                    case Bot.Wall =>        // player on wall -- repelled, some pain
                        val energyDelta = Constants.Energy.PainForPlayerFromHittingWall
                        updatedBoard = updatedBoard.updateBot(movingBot.updateEnergyBy(energyDelta).stunUntil(time + Constants.StunTime.MasterHitsWall))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, state.time, Bonk)
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, state.time, Bonus(energyDelta))
                        bonk()

                    case _ =>
                        assert(false)
                }
            case Bot.GoodBeast =>
                steppedOnBot.variety match {
                    case steppedOnPlayer: Bot.Player =>     // good beast on player -- beast gets eaten
                        updatedBoard = updatedBoard.removeBot(movingBot.id)
                        val energyDelta = Constants.Energy.ValueForPlayerFromEatingGoodBeast
                        updatedBoard = updatedBoard.updateBot(steppedOnBot.updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))

                    case Bot.GoodBeast =>   // good beast on good beast -- repelled
                    case Bot.BadBeast =>    // good beast on bad beast -- repelled
                    case Bot.GoodPlant =>   // good beast on good plant -- repelled
                    case Bot.BadPlant =>    // good beast on bad plant -- repelled
                    case Bot.Wall =>        // good beast on wall -- repelled
                    case _ =>
                        assert(false)
                }
            case Bot.BadBeast =>
                steppedOnBot.variety match {
                    case steppedOnPlayer: Bot.Player => // bad beast on player -- repelled, player gets pain
                        val energyDelta = Constants.Energy.PainForBoth_Player_vs_BadBeast
                        updatedBoard = updatedBoard.updateBot(movingBot.updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.updateBot(steppedOnBot.updateEnergyBy(energyDelta))
                        updatedBoard = updatedBoard.addDecoration(movingBotPos, time, Bonus(energyDelta))

                    case Bot.GoodBeast =>   // bad beast on good beast -- repelled
                    case Bot.BadBeast =>    // bad beast on bad beast -- repelled
                    case Bot.GoodPlant =>   // bad beast on good plant -- repelled
                    case Bot.BadPlant =>    // bad beast on bad plant -- repelled
                    case Bot.Wall =>        // bad beast on wall -- repelled
                    case _ =>
                        assert(false)
                }
            case _ =>   // Bot.GoodPlant, Bot.BadPlant, Bot.Wall
                assert(false)
        }
        updatedBoard
    }
}
