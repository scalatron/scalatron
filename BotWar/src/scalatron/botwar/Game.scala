/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import renderer.Renderer
import scala.concurrent.ExecutionContext
import scalatron.botwar.BotWarSimulation.SimState
import java.awt.event.{WindowEvent, WindowAdapter, KeyEvent, KeyListener}
import scalatron.core._


/** Implementation of the core.Game trait for the Scalatron BotWar game.
  * Since no state is held, this can be a singleton object.
  * It is made available to the Scalatron server via the GameFactory class.
  */
case object Game extends scalatron.core.Game
{
    def gameSpecificPackagePath = "scalatron.botwar.botPlugin"

    def runVisually(rounds: Int, scalatron: ScalatronInward) {
        val argMap = scalatron.argMap

        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(scalatron.secureMode, argMap)

        // pop up the display window, taking command line args into account
        val display = Display.create(argMap)
        display.show()

        // create a renderer; we'll use it to paint the display
        val renderer = Renderer(permanentConfig, scalatron)

        // add a keyboard listener to allow the user to configure the app while it runs
        val keyListener = new KeyListener {
            def keyTyped(e: KeyEvent) {}
            def keyPressed(e: KeyEvent) { renderer.keyPressed(e.getKeyChar) }
            def keyReleased(e: KeyEvent) {}
        }
        display.frame.addKeyListener(keyListener)

        // add a window listener so we can terminate the application if the user closes the window
        var appShouldExit = false
        display.frame.addWindowListener(
            new WindowAdapter() {
                override def windowClosing(e: WindowEvent) {
                    appShouldExit = true
                }
            }
        )

        // determine the maximum frames/second (to throttle CPU usage or sim loop; min: 1 fps)
        val maxFPS = argMap.get("-maxfps").map(_.toInt).getOrElse(50).max(1)
        val minCycleDuration = 1000 / maxFPS    // e.g. 1000/50fps = 20 milliseconds
        var lastCycleTime = System.currentTimeMillis


        // the simulation runner will invoke this callback, which we use to update the display
        val stepCallback = (state: SimState) => {
            scalatron.postStepCallback(state)

            val executionContextForTrustedCode = scalatron.actorSystem.dispatcher
            renderer.draw(display.renderTarget, state.gameState)(executionContextForTrustedCode)

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val currentTime = System.currentTimeMillis
            val cycleDuration = currentTime - lastCycleTime
            lastCycleTime = currentTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)

            !appShouldExit && !renderer.interactivelyAdjustableSettings.abortThisRound
        }


        // now perform game runs ad infinitum
        var roundIndex = 0
        while(!appShouldExit && roundIndex < rounds) {
            // load plugins, either from scratch or incrementally (changed plug-ins only)
            val entityControllers = scalatron.freshEntityControllers

            // update the game configuration based on the plug-ins that are loaded
            val gameConfig = Config.create(permanentConfig, roundIndex, entityControllers, argMap)
            val factory = BotWarSimulation.Factory(gameConfig)

            // prepare a random seed for this game round. Options:
            // (a) time based (completely random game setup for every round)
            // (b) deterministically incremented (beneficial for testing purposes)
            val randomSeed = System.currentTimeMillis.intValue  // or: round

            // run game
            val runner = Simulation.Runner(factory, stepCallback, scalatron.postRoundCallback)
            runner(entityControllers, randomSeed)(scalatron.actorSystem, scalatron.executionContextForUntrustedCode)

            roundIndex += 1

        }

        display.hide()
        display.frame.dispose()
    }


    def runHeadless(
        rounds: Int,
        scalatron: ScalatronInward
    )
    {
        val argMap = scalatron.argMap

        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(scalatron.secureMode, argMap)

        // determine the maximum frames/second (to throttle CPU usage or sim loop; min: 1 fps)
        val maxFPS = argMap.get("-maxfps").map(_.toInt).getOrElse(50).max(1)
        val minCycleDuration = 1000 / maxFPS    // e.g. 1000/50fps = 20 milliseconds
        var lastCycleTime = System.currentTimeMillis

        // the simulation runner will invoke this callback, which we use to update the display
        val stepCallback = (state: SimState) => {
            scalatron.postStepCallback(state)

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val currentTime = System.currentTimeMillis
            val cycleDuration = currentTime - lastCycleTime
            lastCycleTime = currentTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)

            true
        }

        // now perform game runs ad infinitum
        var roundIndex = 0
        while(roundIndex < rounds) {
            val startTime = System.currentTimeMillis()

            // load plugins, either from scratch or incrementally (changed plug-ins only)
            val entityControllers = scalatron.freshEntityControllers

            // update the game configuration based on the plug-ins that are loaded
            val gameConfig = Config.create(permanentConfig, roundIndex, entityControllers, argMap)
            val factory = BotWarSimulation.Factory(gameConfig)

            // prepare a random seed for this game round. Options:
            // (a) time based (completely random game setup for every round)
            // (b) deterministically incremented (beneficial for testing purposes)
            val randomSeed = System.currentTimeMillis.intValue  // or: round

            // run game
            val runner = Simulation.Runner(factory, stepCallback, scalatron.postRoundCallback)
            runner(entityControllers, randomSeed)(scalatron.actorSystem, scalatron.executionContextForUntrustedCode)

            roundIndex += 1

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val endTime = System.currentTimeMillis()
            val cycleDuration = endTime - startTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)
        }
    }


    /** Starts a headless game simulation using the given plug-in collection.
      * @param entityControllers the collection of plug-ins to use as control function factories.
      * @param secureMode if true, certain bot processing restrictions apply
      * @param argMap the command line arguments
      * @return the initial simulation state
      */
    def startHeadless(
        entityControllers: Iterable[EntityController],
        secureMode: Boolean,
        argMap: Map[String,String]
    )(
        executionContextForUntrustedCode: ExecutionContext
    ) : SimState = {
        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(secureMode, argMap)

        val roundIndex = 0

        // determine the per-round configuration for the game
        val gameConfig : Config = Config.create(permanentConfig, roundIndex, entityControllers, argMap)

        // update the game configuration based on the plug-ins that are loaded

        val factory = BotWarSimulation.Factory(gameConfig)

        // prepare a random seed for this game round. Options:
        // (a) time based (completely random game setup for every round)
        // (b) deterministically incremented (beneficial for testing purposes)
        val randomSeed = System.currentTimeMillis.intValue  // or: round

        factory.createInitialState(randomSeed, entityControllers, executionContextForUntrustedCode)
    }


    /** Starts a headless game simulation using the given plug-in collection.
      * @param entityControllers the collection of plug-ins to use as control function factories.
      * @return the initial simulation state
      */
    def startHeadless(
        entityControllers: Iterable[EntityController],
        roundConfig: RoundConfig,
        executionContextForUntrustedCode: ExecutionContext
    ) : SimState = {
        val gameConfig = Config.create(roundConfig.permanent, roundConfig.roundIndex, entityControllers, roundConfig.argMap)

        // update the game configuration based on the plug-ins that are loaded
        val factory = BotWarSimulation.Factory(gameConfig)

        // prepare a random seed for this game round. Options:
        // (a) time based (completely random game setup for every round)
        // (b) deterministically incremented (beneficial for testing purposes)
        val randomSeed = System.currentTimeMillis.intValue  // or: round

        factory.createInitialState(randomSeed, entityControllers, executionContextForUntrustedCode)
    }


    /** Dump the game-specific command line configuration options via println.
      *  "... -x 100 -y 100 -steps 1000" */
    def cmdArgList =
        Iterable("maxfps <int>" -> "maximum steps/second (to reduce CPU load; default: 50)") ++
            Config.cmdArgList ++ Display.cmdArgList
}

