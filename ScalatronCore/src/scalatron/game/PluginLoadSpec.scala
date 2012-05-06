package scalatron.game

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


/** A plug-in loading specification specifies from where plugins should be loaded and which
  * class should be extracted. They are game-specific (e.g. to the game BotWar running within
  * the Scalatron server) and are provided by the game factory.
  */
case class PluginLoadSpec(
    jarFilename: String, // "ScalatronBot.jar"
    gameSpecificPackagePath: String, // "scalatron.botwar.botPlugin"
    factoryClassName: String)

