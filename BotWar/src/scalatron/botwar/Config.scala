/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.core.{EntityController, PermanentConfig}


/** Configuration container for a specific game round. Incorporates the permanent configuration
  * settings by reference.
  */
case class Config(
    permanent: PermanentConfig,     // configuration settings that won't change round-to-round
    boardParams: BoardParams,
    roundIndex: Int
)


object Config {
    def create(permanentConfig: PermanentConfig, roundIndex: Int, entityControllers: Iterable[EntityController], argMap: Map[String,String]) = {
        val entityControllerCount = entityControllers.size

        // compute the default arena size
        val defaultArenaSize = {
            // compute how many cells we want in the arena, depending on the player count
            val defaultCellCount: Int =
                if(entityControllerCount <= 1) 50 * 50
                else if(entityControllerCount <= 2) 60*60
                else if(entityControllerCount <= 3) 70*70
                else if(entityControllerCount <= 4) 85*85
                else if(entityControllerCount <= 5) 100*100
                else if(entityControllerCount <= 8) 120*100
                else if(entityControllerCount <= 10) 150*100
                else if(entityControllerCount <= 15) 180*120
                else if(entityControllerCount <= 20) 200*150
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

        val perimeterWallArg = argMap.getOrElse("-perimeter", "open")
        val perimeter = perimeterWallArg match {
            case "none" => BoardParams.Perimeter.None
            case "open" => BoardParams.Perimeter.Open
            case "closed" => BoardParams.Perimeter.Closed
            case _ =>
                System.err.println("warning: invalid -perimeter option: " + perimeterWallArg)
                BoardParams.Perimeter.Open
        }

        val maxSlaveCountFallback = if(permanentConfig.secureMode) 20 else Int.MaxValue
        val maxSlaveCount = argMap.get("-maxslaves").map(_.toInt).getOrElse(maxSlaveCountFallback)


        val cellCount = boardSize.x * boardSize.y
        val boardParams =
            BoardParams(
                boardSize,
                perimeter,
                wallCount =      argMap.get("-walls").map(_.toInt).getOrElse(cellCount / 300),
                goodPlantCount = argMap.get("-zugars").map(_.toInt).getOrElse(cellCount / 250),
                badPlantCount =  argMap.get("-toxifera").map(_.toInt).getOrElse(cellCount / 350),
                goodBeastCount = argMap.get("-fluppets").map(_.toInt).getOrElse(cellCount / 350),
                badBeastCount =  argMap.get("-snorgs").map(_.toInt).getOrElse(cellCount / 500),
                maxSlaveCount =  maxSlaveCount
            )

        Config(
            permanentConfig,
            boardParams = boardParams,
            roundIndex = roundIndex
        )
    }

    /** Dump the display-specific command line configuration options via println. */
    def cmdArgList = Iterable(
        "x <int>" -> "game arena width (cells; default: depends on plugin count)",
        "y <int>" -> "game arena height (cells; default: depends on plugin count)",
        "perimeter <option>" -> "arena perimeter: none, open, or closed (default: open)",
        "walls <int>" -> "count of wall elements in arena (default: x*y/300)",
        "zugars <int>" -> "count of good plants in arena (default: x*y/250)",
        "toxifera <int>" -> "count of bad plants in arena (default: x*y/350)",
        "fluppets <int>" -> "count of good beasts in arena (default: x*y/350)",
        "snorgs <int>" -> "count of bad beasts in arena (default: x*y/500)"
    )
}




