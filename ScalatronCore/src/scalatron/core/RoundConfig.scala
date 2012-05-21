package scalatron.core

/** Configuration for a specific game round. Incorporates the permanent configuration settings by reference.
  * @param permanent the permanent configuration (number of steps per round, secure mode, etc.)
  * @param argMap the argument settings for this game round (arena size, etc.)
  * @param roundIndex the index of this round within the tournament (zero-based, monotonically increasing)
  */
case class RoundConfig(
    permanent: PermanentConfig,     // configuration settings that won't change round-to-round
    argMap: Map[String,String],
    roundIndex: Int
)
