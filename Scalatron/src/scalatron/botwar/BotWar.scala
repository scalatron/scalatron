/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import renderer.Renderer
import scalatron.botwar.BotWarSimulation.SimState
import java.awt.event.{WindowEvent, WindowAdapter, KeyEvent, KeyListener}
import scalatron.scalatron.api.Scalatron
import scalatron.scalatron.impl.{TournamentRoundResult, TournamentState, Plugin, PluginCollection, Game}
import akka.dispatch.ExecutionContext


/** BotWar: an implementation of the Scalatron Game trait.
  * Main.main() feeds this instance to Scalatron.run(). */
case object BotWar extends Game
{
    val name = Constants.GameName

    val pluginLoadSpec =
        PluginCollection.LoadSpec(
            Scalatron.Constants.JarFilename,          // "ScalatronBot.jar"
            "scalatron.botwar.botPlugin",
            "ControlFunctionFactory")


    def runVisually(
        pluginPath: String,
        argMap: Map[String,String],
        rounds: Int,
        tournamentState: TournamentState,   // receives tournament round results
        secureMode: Boolean,
        verbose: Boolean
    )(
        executionContextForTrustedCode: ExecutionContext,
        executionContextForUntrustedCode: ExecutionContext
    )
    {
        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(secureMode, argMap)

        // pop up the display window, taking command line args into account
        val display = Display.create(argMap)
        display.show()

        // create a renderer; we'll use it to paint the display
        val renderer = Renderer(permanentConfig, tournamentState)

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
            tournamentState.updateMostRecentState(state)

            renderer.draw(display.renderTarget, state.gameState)(executionContextForTrustedCode)

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val currentTime = System.currentTimeMillis
            val cycleDuration = currentTime - lastCycleTime
            lastCycleTime = currentTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)

            !appShouldExit && !renderer.interactivelyAdjustableSettings.abortThisRound
        }

        // the simulation runner will invoke this callback, which we use to update the display
        val resultCallback = (state: SimState, tournamentRoundResult: TournamentRoundResult) => {
            tournamentState.updateMostRecentState(state)
            tournamentState.addResult(tournamentRoundResult)
        }


        var pluginCollection = PluginCollection(pluginPath, pluginLoadSpec, verbose)

        // now perform game runs ad infinitum
        var roundIndex = 0
        while(!appShouldExit && roundIndex < rounds) {
            // load plugins, either from scratch or incrementally (changed plug-ins only)
            pluginCollection = pluginCollection.incrementalRescan // or: fullRescan
            val plugins = pluginCollection.plugins

            // update the game configuration based on the plug-ins that are loaded
            val gameConfig = Config.create(permanentConfig, roundIndex, plugins, argMap)
            val factory = BotWarSimulation.Factory(gameConfig)

            // prepare a random seed for this game round. Options:
            // (a) time based (completely random game setup for every round)
            // (b) deterministically incremented (beneficial for testing purposes)
            val randomSeed = System.currentTimeMillis.intValue  // or: round

            // run game
            val runner = Simulation.Runner(factory, stepCallback, resultCallback)
            runner(plugins, randomSeed)(executionContextForTrustedCode, executionContextForUntrustedCode)

            roundIndex += 1

        }

        display.hide()
        display.frame.dispose()
    }


    def runHeadless(
        pluginPath: String,
        argMap: Map[String,String],
        rounds: Int,
        tournamentState: TournamentState,   // receives tournament round results
        secureMode: Boolean,
        verbose: Boolean
    )(
        executionContextForTrustedCode: ExecutionContext,
        executionContextForUntrustedCode: ExecutionContext
    )
    {
        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(secureMode, argMap)

        // determine the maximum frames/second (to throttle CPU usage or sim loop; min: 1 fps)
        val maxFPS = argMap.get("-maxfps").map(_.toInt).getOrElse(50).max(1)
        val minCycleDuration = 1000 / maxFPS    // e.g. 1000/50fps = 20 milliseconds
        var lastCycleTime = System.currentTimeMillis

        // the simulation runner will invoke this callback, which we use to update the display
        val stepCallback = (state: SimState) => {
            tournamentState.updateMostRecentState(state)

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val currentTime = System.currentTimeMillis
            val cycleDuration = currentTime - lastCycleTime
            lastCycleTime = currentTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)

            true
        }

        // the simulation runner will invoke this callback, which we use to update the tournament state
        val resultCallback = (state: SimState, tournamentRoundResult: TournamentRoundResult) => {
            tournamentState.updateMostRecentState(state)
            tournamentState.addResult(tournamentRoundResult)
        }

        var pluginCollection = PluginCollection(pluginPath, pluginLoadSpec, verbose)

        // now perform game runs ad infinitum
        var roundIndex = 0
        while(roundIndex < rounds) {
            val startTime = System.currentTimeMillis()

            // load plugins, either from scratch or incrementally (changed plug-ins only)
            pluginCollection = pluginCollection.incrementalRescan // or: fullRescan
            val plugins = pluginCollection.plugins

            // update the game configuration based on the plug-ins that are loaded
            val gameConfig = Config.create(permanentConfig, roundIndex, plugins, argMap)
            val factory = BotWarSimulation.Factory(gameConfig)

            // prepare a random seed for this game round. Options:
            // (a) time based (completely random game setup for every round)
            // (b) deterministically incremented (beneficial for testing purposes)
            val randomSeed = System.currentTimeMillis.intValue  // or: round

            // run game
            val runner = Simulation.Runner(factory, stepCallback, resultCallback)
            runner(plugins, randomSeed)(executionContextForTrustedCode, executionContextForUntrustedCode)

            roundIndex += 1

            // enforce maxFPS (max frames per second) by putting this thread to sleep if appropriate
            val endTime = System.currentTimeMillis()
            val cycleDuration = endTime - startTime
            val timeToWait = minCycleDuration - cycleDuration    // e.g. 20ms - 10ms = 10ms
            if(timeToWait > 0) Thread.sleep(timeToWait)
        }
    }


    /** Starts a headless game simulation using the given plug-in collection.
      * @param plugins the collection of plug-ins to use as control function factories.
      * @param secureMode if true, certain bot processing restrictions apply
      * @param argMap the command line arguments
      * @return the initial simulation state
      */
    def startHeadless(
        plugins: Iterable[Plugin.External],
        secureMode: Boolean,
        argMap: Map[String,String]
    )(
        executionContextForUntrustedCode: ExecutionContext
    ) : SimState = {
        // determine the permanent configuration for the game
        val permanentConfig = PermanentConfig.fromArgMap(secureMode, argMap)

        val roundIndex = 0

        // determine the per-round configuration for the game
        val gameConfig : Config = Config.create(permanentConfig, roundIndex, plugins, argMap)

        // update the game configuration based on the plug-ins that are loaded

        val factory = BotWarSimulation.Factory(gameConfig)

        // prepare a random seed for this game round. Options:
        // (a) time based (completely random game setup for every round)
        // (b) deterministically incremented (beneficial for testing purposes)
        val randomSeed = System.currentTimeMillis.intValue  // or: round

        factory.createInitialState(randomSeed, plugins)(executionContextForUntrustedCode)
    }


    /** Starts a headless game simulation using the given plug-in collection.
      * @param plugins the collection of plug-ins to use as control function factories.
      * @return the initial simulation state
      */
    def startHeadless(
        plugins: Iterable[Plugin.External],
        permanentConfig: PermanentConfig,
        gameConfig: Config
    )(
        executionContextForUntrustedCode: ExecutionContext
    ) : SimState = {
        // update the game configuration based on the plug-ins that are loaded
        val factory = BotWarSimulation.Factory(gameConfig)

        // prepare a random seed for this game round. Options:
        // (a) time based (completely random game setup for every round)
        // (b) deterministically incremented (beneficial for testing purposes)
        val randomSeed = System.currentTimeMillis.intValue  // or: round

        factory.createInitialState(randomSeed, plugins)(executionContextForUntrustedCode)
    }


    /** Dump the game-specific command line configuration options via println.
      *  "... -x 100 -y 100 -steps 1000" */
    def printArgList() {
        Config.printArgList()
        PermanentConfig.printArgList()
        Display.printArgList()
        Renderer.printKeyboardCommands()

        println("  -maxfps <int>            maximum steps/second (to reduce CPU load; default: 50)")
    }
}

