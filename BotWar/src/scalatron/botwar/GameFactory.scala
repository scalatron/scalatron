package scalatron.botwar

import scalatron.core.Game

/** This is the class that will be extracted from the game plug-in by Scalatron in order to obtain a
  * game factory function.
  */
class GameFactory {
    /** This is game factory function that Scalatron will use to instantiate the game instance. */
    def create() : Game = BotWar
}
