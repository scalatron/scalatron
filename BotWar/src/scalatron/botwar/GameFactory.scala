package scalatron.botwar

import scalatron.game.Game

/** This is the factory used by the Scalatron Game plug-in loader to instantiate the game.
  */
class GameFactory
{
    def create() : Game = BotWar
}
