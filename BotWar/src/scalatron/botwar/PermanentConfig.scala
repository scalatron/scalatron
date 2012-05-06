/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.game.Plugin


/** Configuration settings that won't change round-to-round */
case class PermanentConfig(
    stepsPerRound: Int,                                     // number of steps to run the game
    internalPlugins: Iterable[Plugin.Internal]              // for debugging within the server
    )


object PermanentConfig {
    def fromArgMap(argMap: Map[String,String]) = {
        val steps = argMap.get("-steps").map(_.toInt).getOrElse(5000)

        PermanentConfig(
            stepsPerRound = steps,
            internalPlugins = Iterable(
                // Plugin.Internal(":SimpleBot", () => new SimpleBot().respond _)
            )
        )
    }

    /** Dump the display-specific command line configuration options via println. */
    def printArgList() {
        println("  -steps <int>             steps per game cycle (default: 5000)")
    }

}
