/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.core

/** Configuration settings that won't change round-to-round
  * @param secureMode if true, certain bot processing restrictions apply
  * @param stepsPerRound number of steps to run the game
  */
case class PermanentConfig(secureMode: Boolean, stepsPerRound: Int)


object PermanentConfig
{
    def fromArgMap(secureMode: Boolean, argMap: Map[String, String]) = {
        val steps = argMap.get("-steps").map(_.toInt).getOrElse(5000)
        PermanentConfig(secureMode = secureMode, stepsPerRound = steps)
    }

    /** Dump the display-specific command line configuration options via println. */
    def cmdArgList = Iterable("steps <int>" -> "steps per game cycle (default: 5000)")
}
