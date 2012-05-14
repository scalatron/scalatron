/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.util.Random
import scalatron.scalatron.impl.TournamentRoundResult
import akka.util.duration._
import akka.dispatch._
import java.util.concurrent.TimeoutException
import akka.actor.ActorSystem


/** Game dynamics. Function that, when applied to a game state, returns either a successor
  *  game state or a game result.
  */
case object Dynamics extends ((State, Random, ActorSystem, ExecutionContext) => Either[State,TournamentRoundResult])
{
    def apply(state: State, rnd: Random, actorSystem: ActorSystem, executionContextForUntrustedCode: ExecutionContext) = {
        // determine which bots are eligible to move in the nest step
        val eligibleBots = computeEligibleBots(state)

        // have each bot compute its command
        val botCommandsAndTimes = computeBotCommands(state, eligibleBots, actorSystem)(executionContextForUntrustedCode) // was: actorSystem

        // store time taken with each bot
        var updatedBoard = state.board
        val botCommands = botCommandsAndTimes.map(tuple => {
            val id = tuple._1
            val commands = tuple._2._3

            // if it is a player, update its CPU time and input string
            updatedBoard.getBot(id) match {
                case None => // ?!?
                case Some(bot) => bot.variety match {
                    case player: Bot.Player =>
                        val nanoSeconds = tuple._2._1
                        // update CPU time & input string
                        val updatedPlayer = player.copy(
                            cpuTime = player.cpuTime + nanoSeconds,
                            controlFunctionInput = tuple._2._2,
                            controlFunctionOutput = commands,
                            stateMap = player.stateMap - Protocol.PropertyName.Debug - Protocol.PropertyName.Bonked
                        )
                        val updatedBot = bot.updateVariety(updatedPlayer)
                        updatedBoard = updatedBoard.updateBot(updatedBot)
                        if(!player.isMaster) {
                            // for slaves, update master's CPU time
                            val masterId = player.masterId
                            updatedBoard.getBot(masterId) match {
                                case None => // ?!?
                                case Some(masterBot) => masterBot.variety match {
                                    case masterPlayer: Bot.Player =>
                                        assert(masterPlayer.isMaster)
                                        val updatedPlayer = masterPlayer.copy(cpuTime = masterPlayer.cpuTime + nanoSeconds)
                                        val updatedBot = masterBot.updateVariety(updatedPlayer)
                                        updatedBoard = updatedBoard.updateBot(updatedBot)
                                    case _ => // OK: beast
                                }
                            }
                        }
                    case _ => // OK: beast
                }
            }

            (id, commands)
        })
        val updatedState = state.copy(board = updatedBoard)

        // apply the commands to the game state, as well as regular dynamics (e.g. physics)
        AugmentedDynamics(updatedState, rnd, botCommands)
    }


    /** Compute the subset of entities that will be allowed to move this cycle.
      * Note that we would be able to parallelize this, although the gain would be negligible.
      */
    def computeEligibleBots(state: State): Iterable[Bot] =
        state.board.botsFiltered(bot =>
            if(bot.stunnedUntil > state.time) {
                false
            } else {
                bot.variety match {
                    case player: Bot.Player =>
                        if(player.isMaster) (state.time % 2L) == 0L    // only every second cycle
                        else true
                    case Bot.BadBeast => (state.time % 4L) == (bot.id % 4)    // only every fourth cycle, but not all at same time
                    case Bot.GoodBeast => (state.time % 4L) == (bot.id % 4)   // only every fourth cycle, but not all at same time
                    case _ => false
                }
            })

    type BotResponse = (Entity.Id,(Long,String,Iterable[Command]))

    // returns a collection of tuples: (id, (nanoSeconds, commandList))
    def computeBotCommands(state: State, eligibleBots: Iterable[Bot], actorSystem: ActorSystem)(implicit executionContextForUntrustedCode: ExecutionContext): Iterable[(Entity.Id,(Long,String,Iterable[Command]))] = {
        // use Akka Futures to work this out
        val outerFuture = Future.traverse(eligibleBots)(bot => Future {
            try {
                val timeBefore = System.nanoTime
                val (inputString,commands) = bot.respondTo(state)
                val timeSpent = System.nanoTime - timeBefore
                if(commands.isEmpty) None else Some((bot.id,(timeSpent,inputString,commands)))
            } catch {
                case t: NoClassDefFoundError =>
                    // we fake a Log() command issued by the bot to report the error into the browser UI:
                    Some((bot.id,(0L,"",Iterable[Command](Command.Disable("error: class not found: " + t.getMessage)))))

                case t: Throwable =>
                    // we inject a Disable() command as-if-issued-by-the-bot to report the error into the browser UI:
                    Some((bot.id,(0L,"",Iterable[Command](Command.Disable(t.getMessage)))))
            }
        })


        /*
        // This is based on Viktor Klang's proposal, see https://groups.google.com/forum/?fromgroups#!topic/akka-user/5MY8lsYGbYM
        val TimeoutSentinel : Option[BotResponse] = None
        val timeout = Promise[Option[BotResponse]]()
        val c = actorSystem.scheduler.scheduleOnce(1000 millis){ timeout.success(TimeoutSentinel) }
        timeout onComplete { case _ => c.cancel() }

        val outerFuture = Future.traverse(eligibleBots)(bot =>
            Promise[Option[BotResponse]]()
            .completeWith(Future {
                try {
                    val timeBefore = System.nanoTime
                    val (inputString,commands) = bot.respondTo(state)
                    val timeSpent = System.nanoTime - timeBefore
                    if(commands.isEmpty) None else Some((bot.id,(timeSpent,inputString,commands)))
                } catch {
                    case t: NoClassDefFoundError =>
                        // we fake a Log() command issued by the bot to report the error into the browser UI:
                        Some((bot.id,(0L,"",Iterable[Command](Command.Disable("error: class not found: " + t.getMessage)))))

                    case t: Throwable =>
                        // we inject a Disable() command as-if-issued-by-the-bot to report the error into the browser UI:
                        Some((bot.id,(0L,"",Iterable[Command](Command.Disable(t.getMessage)))))
                }
            })
            .completeWith(timeout)
        )
        */
        /*
        // this is an attempt based on nested Futures. Does not seems to work at all: every call times out. ?!?
        val TimeoutSentinel : Option[BotResponse] = None
        val outerFuture = Future.traverse(eligibleBots)(bot =>
            Future {
                val innerFuture = Future {
                    try {
                        val timeBefore = System.nanoTime
                        val (inputString,commands) = bot.respondTo(state)
                        val timeSpent = System.nanoTime - timeBefore
                        if(commands.isEmpty) None else Some((bot.id,(timeSpent,inputString,commands)))
                    } catch {
                        case t: NoClassDefFoundError =>
                            // we fake a Log() command issued by the bot to report the error into the browser UI:
                            Some((bot.id,(0L,"",Iterable[Command](Command.Disable("error: class not found: " + t.getMessage)))))

                        case t: Throwable =>
                            // we inject a Disable() command as-if-issued-by-the-bot to report the error into the browser UI:
                            Some((bot.id,(0L,"",Iterable[Command](Command.Disable(t.getMessage)))))
                    }
                }
                try {
                    Await.result(innerFuture, 5000 millis)      // generous timeout - for now, we only want to capture infinite loops
                } catch {
                    case t: TimeoutException =>
                        System.err.println("warning: timeout while invoking the control function of bot: " + bot.name)
                        TimeoutSentinel
                }
            }
        )
        */

        // Note: an overall timeout across all bots is a temporary solution - we want timeouts PER BOT
        try {
            val result = Await.result(outerFuture, 10000 millis)      // generous timeout - note that this is over ALL plug-ins
            // result.filter(TimeoutSentinel ==).foreach(r => println("bot timed out"))
            result.flatten
        } catch {
            case t: TimeoutException =>
                System.err.println("warning: timeout while invoking the control function of one of the plugins")
                Iterable.empty          // temporary - disables ALL bots, which is not the intention
        }
    }


    // returns an Option containing tuples: (id, (nanoSeconds, inputString, commandList))
    def computeBotCommandOpt(bot: Bot, state: State) : Option[(Entity.Id,(Long,String,Iterable[Command]))] =
        try {
            val timeBefore = System.nanoTime
            val (inputString,commands) = bot.respondTo(state)
            val timeSpent = System.nanoTime - timeBefore
            if(commands.isEmpty) None else Some((bot.id,(timeSpent,inputString,commands)))
        } catch {
            case t: NoClassDefFoundError =>
                // System.err.println("Bot '" + bot.name + "' caused an error: " + t);

                // we fake a Log() command issued by the bot to report the error into the browser UI:
                Some((bot.id,(0L,"",Iterable[Command](Command.Log("error: class not found: " + t.getMessage)))))

            case t: Throwable =>
                System.err.println("Bot '" + bot.name + "' caused an error: " + t);

                // we fake a Log() command issued by the bot to report the error into the browser UI:
                Some((bot.id,(0L,"",Iterable[Command](Command.Log(t.getMessage)))))
        }
}



