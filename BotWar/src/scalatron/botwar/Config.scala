/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.game.Plugin


/** Configuration container for a specific game rounds. Incorporates the permanent configuration
  * settings by reference.
  */
case class Config(
    permanent: PermanentConfig,     // configuration settings that won't change round-to-round
    boardParams: BoardParams,
    roundIndex: Int
    )


object Config {
    def create(permanentConfig: PermanentConfig, roundIndex: Int, plugins: Iterable[Plugin.External], argMap: Map[String,String]) = {
        // cell count on the game board dependents on the number of plug-ins that are loaded
        val internalPluginCount = permanentConfig.internalPlugins.size
        val externalPluginCount = plugins.size
        val pluginCount = internalPluginCount + externalPluginCount

        // compute the default arena size
        val defaultArenaSize = {
            // compute how many cells we want in the arena, depending on the player count
            val defaultCellCount: Int =
                if(pluginCount <= 1) 50 * 50
                else if(pluginCount <= 2) 60*60
                else if(pluginCount <= 3) 70*70
                else if(pluginCount <= 4) 85*85
                else if(pluginCount <= 5) 100*100
                else if(pluginCount <= 8) 120*100
                else if(pluginCount <= 10) 150*100
                else if(pluginCount <= 15) 180*120
                else if(pluginCount <= 20) 200*150
                else 200*200

            // for the moment, we convert the cell count into a square arena layout;
            // in a future version, we could take the screen layout into account to minimize
            // unused space on the screen
            val edgeLength = math.sqrt(defaultCellCount).intValue
            XY(edgeLength, edgeLength)
        }

        val cellCountX = argMap.get("-x").map(_.toInt).getOrElse(defaultArenaSize.x).max(40)
        val cellCountY = argMap.get("-y").map(_.toInt).getOrElse(defaultArenaSize.y).max(40)
        val boardSize = XY(cellCountX, cellCountY)

        val perimeterWallArg = argMap.get("-perimeter").getOrElse("open")
        val perimeter = perimeterWallArg match {
            case "none" => BoardParams.Perimeter.None
            case "open" => BoardParams.Perimeter.Open
            case "closed" => BoardParams.Perimeter.Closed
            case _ =>
                System.err.println("warning: invalid -perimeter option: " + perimeterWallArg)
                BoardParams.Perimeter.Open
        }


        val cellCount = boardSize.x * boardSize.y
        val boardParams =
            BoardParams(
                boardSize,
                perimeter,
                wallCount =      argMap.get("-walls").map(_.toInt).getOrElse(cellCount / 300),
                goodPlantCount = argMap.get("-zugars").map(_.toInt).getOrElse(cellCount / 250),
                badPlantCount =  argMap.get("-toxifera").map(_.toInt).getOrElse(cellCount / 350),
                goodBeastCount = argMap.get("-fluppets").map(_.toInt).getOrElse(cellCount / 350),
                badBeastCount =  argMap.get("-snorgs").map(_.toInt).getOrElse(cellCount / 500)
            )

        Config(
            permanentConfig,
            boardParams = boardParams,
            roundIndex = roundIndex
        )
    }

    /** Dump the display-specific command line configuration options via println. */
    def printArgList() {
        println("  -x <int>                 game arena width (cells; default: depends on plugin count)")
        println("  -y <int>                 game arena height (cells; default: depends on plugin count)")
        println("  -perimeter <option>      arena perimeter: none, open, or closed (default: open)")
        println("  -walls <int>             count of wall elements in arena (default: x*y/300)")
        println("  -zugars <int>            count of good plants in arena (default: x*y/250)")
        println("  -toxifera <int>          count of bad plants in arena (default: x*y/350)")
        println("  -fluppets <int>          count of good beasts in arena (default: x*y/350)")
        println("  -snorgs <int>            count of bad beasts in arena (default: x*y/500)")
    }
}




