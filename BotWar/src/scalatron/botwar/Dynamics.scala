/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scala.concurrent.{Await, Future, ExecutionContext}
import scala.util.Random
import scala.concurrent.duration._
import akka.actor.ActorSystem
import scalatron.core.TournamentRoundResult


/** Game dynamics. Function that, when applied to a game state, returns either a successor
  *  game state or a game result.
  */
case object Dynamics extends ((State, Random, ActorSystem, ExecutionContext) => Either[State,TournamentRoundResult])
{
    def apply(state: State, rnd: Random, actorSystem: ActorSystem, executionContextForUntrustedCode: ExecutionContext) = {
        // determine which bots are eligible to move in the nest step
        val eligibleBots = computeEligibleBots(state)

        // have each bot compute its command
        val botCommandsAndTimes = computeBotCommands(state, eligibleBots, actorSystem, executionContextForUntrustedCode)

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

                        // update the bot:
                        // - record how much CPU time it used
                        // - record its control function input and output strings (for display in browser UI)
                        // - remove ephemeral values from the state parameter map: debug output & collision state
                        val updatedPlayer = player.copy(
                            cpuTime = player.cpuTime + nanoSeconds,
                            controlFunctionInput = tuple._2._2,
                            controlFunctionOutput = commands,
                            stateMap = player.stateMap - Protocol.PropertyName.Debug - Protocol.PropertyName.Collision
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
    def computeBotCommands(
        state: State,
        eligibleBots: Iterable[Bot],
        actorSystem: ActorSystem,
        executionContextForUntrustedCode: ExecutionContext
    ): Iterable[BotResponse] =
    {
        // break the bots up into two pools: trusted (computer-controlled) and untrusted (plug-in-controlled)
        val (untrustedBots, trustedBots) = eligibleBots.partition(_.isUntrusted)

        def computeBotResponse(bot: Bot) : Option[BotResponse] =
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

        // concurrently compute trusted and untrusted bots using two "outer" futures, one for each group:
        val trustedFuture = {
            implicit val executionContext = actorSystem.dispatcher  // for trusted code
            Future.traverse(trustedBots)(bot => Future { computeBotResponse(bot) })
        }

        val TimeoutForEachUntrustedBot : Duration = 1000 millis

        val untrustedFuture = {
            implicit val executionContext = executionContextForUntrustedCode

/*
            // Option A: promise with timeout fallback
            // This is based on Viktor Klang's proposal, see https://groups.google.com/forum/?fromgroups#!topic/akka-user/5MY8lsYGbYM
            val timeout = Promise[Option[BotResponse]]()
            val c = actorSystem.scheduler.scheduleOnce(TimeoutForEachUntrustedBot){ timeout.success(TimeoutSentinel) }
            timeout onComplete { case _ => c.cancel() }

            Future.traverse(untrustedBots)(bot =>
                Promise[Option[BotResponse]]()
                .completeWith(Future { computeBotResponse(bot) })
                .completeWith(timeout)
            )
*/
/*
            // Option B: nested futures - does not really work
            Future.traverse(untrustedBots)(bot => Future {
                val innerFuture = Future { computeBotResponse(bot) }
                try {
                    Await.result(innerFuture, TimeoutForEachUntrustedBot)
                } catch {
                    case t: TimeoutException =>
                        System.err.println("warning: bot disabled because of timeout: " + bot.name)
                        // we inject a Disable() command as-if-issued-by-the-bot to report the error into the browser UI:
                        Some((bot.id,(0L,"",Iterable[Command](Command.Disable(t.getMessage)))))
                }
            })
*/
            // Option C: regular futures with overall timout
            Future.traverse(untrustedBots)(bot => Future { computeBotResponse(bot) })
        }

        // Note: an overall timeout across all bots is a temporary solution - we want timeouts PER BOT
        val trustedBotResponseOpts = Await.result(trustedFuture, Duration.Inf)
        val trustedBotResponses = trustedBotResponseOpts.flatten

        val untrustedBotResponseOpts = Await.result(untrustedFuture, Duration.Inf) // generous timeout - note that this is over ALL plug-ins
        val untrustedBotResponses = untrustedBotResponseOpts.flatten

        trustedBotResponses ++ untrustedBotResponses
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



