/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.core.{Simulation, EntityController}
import Simulation.Time
import scala.util.Random
import BoardParams.Perimeter
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException


/** Contains the temporally variable aspects of the game state.
  */
case class Board(
    nextId: Entity.Id,
    decorations: Map[Entity.Id, Decoration],                        // non-colliding
    private val bots: Map[Entity.Id, Bot])                          // colliding
{
    // access to private bots Map only via these methods -- allows us to do some caching
    def botCount : Int = bots.size
    def getBot(id: Entity.Id) : Option[Bot] = bots.get(id)
    def botsNear(center: XY, radius: Double) : Iterable[Bot] = bots.values.filter(_.pos.distanceTo(center) <= radius)
    def botsFiltered(p: Bot => Boolean) : Iterable[Bot] = bots.values.filter(p)
    def botsForEach(f: Bot => Unit) { bots.values.foreach(f) }
    def botsThatAreWallsAndNonWalls : (Iterable[Bot],Iterable[Bot]) = { bots.values.partition(_.variety == Bot.Wall) }
    lazy val botsThatArePlayers = bots.values.filter(bot => bot.variety.isInstanceOf[Bot.Player])       // Optimization 2012-02-27
    def botsThatAreMasters = bots.values.filter(bot => bot.isMaster)      // generation-0 player objects
    def botsThatAreSlaves = bots.values.filter(bot => bot.isSlave)        // generation-1+ player objects
    def botsThatAreSlavesOrBeasts = bots.values.filter(bot => bot.isSlave || bot.variety == Bot.GoodBeast || bot.variety == Bot.BadBeast)
    def botsThatAreGoodPlants = bots.values.filter(bot => bot.variety == Bot.GoodPlant)
    def botsThatAreBadPlants = bots.values.filter(bot => bot.variety == Bot.BadPlant)
    def botsThatAreGoodBeasts = bots.values.filter(bot => bot.variety == Bot.GoodBeast)
    def botsThatAreBadBeasts = bots.values.filter(bot => bot.variety == Bot.BadBeast)

    def nearestBotThatIsPlayer(pos: XY) : Option[Bot] = {
        val players = botsThatArePlayers                                // inefficient :-(
        if(players.isEmpty) None
        else Some(players.minBy(_.pos.stepsTo(pos)))
    }

    /** Returns the master bot of the player with the given name. */
    def botThatIsMasterOfPlayer(name: String) : Option[Bot] = {
        botsThatArePlayers.find(bot =>
            bot.variety match {
                case player: Bot.Player => player.isMaster && player.name==name
                case _ => false
            })
    }

    /** Returns all bots and mini-bots (master & slaves) of the player with the given name. */
    def entitiesOfPlayer(name: String) : Iterable[Bot] = {
        botThatIsMasterOfPlayer(name) match {
            case None => Iterable.empty     // no master for this player name
            case Some(master) =>
                val masterId = master.id
                botsThatArePlayers.filter(bot =>
                    bot.variety match {
                        case player: Bot.Player => player.masterId == masterId
                        case _ => false

                    })
        }
    }

    /** Returns the master bot and all mini-bots that are siblings of the given bot. */
    def siblingsOfBot(bot: Bot) : Iterable[Bot] =
        bot.variety match {
            case thisPlayer: Bot.Player =>
                botsThatArePlayers.filter(bot =>
                    bot.variety match {
                        case player: Bot.Player => player.masterId == thisPlayer.masterId
                        case _ => false
                    })
            case _ => Iterable.empty
        }



    def addDecoration(pos: XY, creationTime: Time, variety: Decoration.Variety) =
        copy(nextId = nextId + 1, decorations = decorations.updated(nextId, Decoration(nextId, pos, creationTime, variety)))

    def addBot(pos: XY, extent: XY, creationTime: Time, variety: Bot.Variety) : Board =
        addBot(pos, extent, creationTime, Constants.Energy.Initial, variety)

    def addBot(pos: XY, extent: XY, creationTime: Time, energy: Int, variety: Bot.Variety) : Board =
        copy(nextId = nextId + 1, bots = bots.updated(nextId, Bot(nextId, pos, extent, creationTime, Time.SomtimeInThePast, energy, variety)))

    def addBotThatIsMaster(pos: XY, creationTime: Time, entityController: EntityController) : Board =
        copy(nextId = nextId + 1, bots = bots.updated(nextId,
            Bot(nextId, pos, XY.One, creationTime, Time.SomtimeInThePast, Constants.Energy.Initial,
                Bot.Player(entityController, Bot.MasterGeneration, nextId, (0,2), 0L, "", Iterable.empty, Map(Protocol.PropertyName.Name -> entityController.name))
        )))

    def sprinkle(count: Int, rnd: Random, creationTime: Time, boardSize: XY, variety: Bot.Variety) =
        (this /: (0 until count))((sum,n) => sum.addBot(sum.emptyRandomPos(rnd, boardSize), XY.One, creationTime, variety))
    def sprinkleGoodPlants(count: Int, rnd: Random, creationTime: Time, boardSize: XY) = sprinkle(count, rnd, creationTime, boardSize, Bot.GoodPlant)
    def sprinkleBadPlants(count: Int, rnd: Random, creationTime: Time, boardSize: XY) = sprinkle(count, rnd, creationTime, boardSize, Bot.BadPlant)
    def sprinkleGoodBeasts(count: Int, rnd: Random, creationTime: Time, boardSize: XY) = sprinkle(count, rnd, creationTime, boardSize, Bot.GoodBeast)
    def sprinkleBadBeasts(count: Int, rnd: Random, creationTime: Time, boardSize: XY) = sprinkle(count, rnd, creationTime, boardSize, Bot.BadBeast)



    def removeBot(id: Entity.Id) = copy(bots = bots - id)
    def removeDecoration(id: Entity.Id) = copy(decorations = decorations - id)

    def updateBot(bot: Bot) = copy(bots = bots.updated(bot.id, bot))



    def randomPos(rnd: Random, boardSize: XY) = XY(rnd.nextInt(boardSize.x), rnd.nextInt(boardSize.y))
    def emptyRandomPos(rnd: Random, boardSize: XY) = {
        var pos = randomPos(rnd, boardSize)
        while( isOccupied(pos) ) pos = randomPos(rnd, boardSize)
        pos
    }
    def isValidAndVacant(pos: XY, boardSize: XY) : Boolean =
        pos.x >= 0 && pos.y >= 0 && pos.x < boardSize.x && pos.y < boardSize.y && isVacant(pos)


    def isOccupied(pos: XY) = occupant(pos).isDefined
    def isVacant(pos: XY) = occupant(pos).isEmpty
    def occupant(pos: XY) = bots.values.find(_.isAt(pos))

    def flatten(boardSize: XY) = {
        val boardSizeX = boardSize.x
        val cells = Array.fill[Option[Bot]](boardSizeX * boardSize.y)(None)
        bots.values.foreach( bot => {
            val extent = bot.extent
            if(extent eq XY.One) {     // a little optimization
                val index = bot.pos.x + bot.pos.y * boardSizeX
                assert(cells(index).isEmpty, "cell with double occupancy: " + cells(index).get + " and " + bot)
                cells(index) = Some(bot)
            } else {
                for(ex <- 0 until extent.x) {
                    for(ey <- 0 until extent.y) {
                        val index = (bot.pos.x + ex) + (bot.pos.y + ey) * boardSizeX
                        assert(cells(index).isEmpty, "cell with double occupancy: " + cells(index).get + " and " + bot)
                        cells(index) = Some(bot)
                    }
                }
            }
        })
        FlattenedBoard(boardSize, cells)
    }
}

object Board
{
    val Empty = Board(0, Map.empty, Map.empty)

    def createInitial(
        boardParams: BoardParams,
        time: Time,
        stepsPerRound: Int,
        roundIndex: Int,
        randomSeed: Int,
        entityControllers: Iterable[EntityController]
    )(
        implicit executionContextForUntrustedCode: ExecutionContext
    )
    = {
        val boardSize = boardParams.size
        val maxSlaves = boardParams.maxSlaveCount

        var updatedBoard = Empty
        val rnd = new Random(randomSeed)

        updatedBoard = spawnPerimeterWalls(boardParams.perimeter, updatedBoard, time, boardSize)
        updatedBoard = spawnRandomWalls(rnd, updatedBoard, time, boardSize, boardParams.wallCount)

        // initialize the entity controllers' control functions -- use Akka to work this out
        // isolate the control function factory invocation via the untrusted thread pool
        val future = Future.traverse(entityControllers)(entityController => Future {
            try {
                // invoke Welcome()
                entityController.respond(
                    Protocol.ServerOpcode.Welcome + "(" +
                        "name=" + entityController.name + "," +
                        "apocalypse=" + stepsPerRound + "," +
                        "round=" + roundIndex +
                        (if (maxSlaves < Int.MaxValue) ",maxslaves=" + maxSlaves else "") +
                        ")")
            } catch {
                case t: Throwable =>
                    System.err.println("error: exception while instantiating control function of plugin '" + entityController.name + "': " + t)
                    None
            }
        })

        // Note: an overall timeout across all bots is a temporary solution - we want timeouts PER BOT
        try {
            Await.result(future, 2000 millis)      // generous timeout - note that this is over ALL plug-ins
        } catch {
            case t: TimeoutException =>
                System.err.println("warning: timeout while instantiating control function of one of the plugins")
                Iterable.empty          // temporary - disables ALL bots, which is not the intention
        }

        // spawn players
        entityControllers.foreach( entityController => {
            val position = updatedBoard.emptyRandomPos(rnd, boardSize)
            updatedBoard = updatedBoard.addBotThatIsMaster(position, time, entityController)
        } )

        updatedBoard
    }


    def spawnPerimeterWalls(perimeter: Perimeter, board: Board, time: Time, boardSize: XY) : Board =
        perimeter match {
            case Perimeter.None =>
                board

            case Perimeter.Open =>
                val sx = boardSize.x; val sy = boardSize.y
                val lx = sx/2-12; val ly = sy/2-10
                val ex = XY(lx,1); val ey = XY(1,ly)

                board
                .addBot(XY(0,0), ex, time, Bot.Wall)                    // top left
                .addBot(XY(sx-lx,0), ex, time, Bot.Wall)                // top right
                .addBot(XY(0,sy-1), ex, time, Bot.Wall)                 // bottom left
                .addBot(XY(sx-lx,sy-1), ex, time, Bot.Wall)             // bottom right
                .addBot(XY(0,1), ey, time, Bot.Wall)                    // left top
                .addBot(XY(0,sy-ly-1), ey, time, Bot.Wall)              // left bottom
                .addBot(XY(boardSize.x-1,1), ey, time, Bot.Wall)        // right top
                .addBot(XY(sx-1,sy-ly-1), ey, time, Bot.Wall)           // right top

            case Perimeter.Closed =>
                board
                .addBot(XY(0,0), XY(boardSize.x,1), time, Bot.Wall)
                .addBot(XY(0,boardSize.y-1), XY(boardSize.x,1), time, Bot.Wall)
                .addBot(XY(0,1), XY(1,boardSize.y-2), time, Bot.Wall)
                .addBot(XY(boardSize.x-1,1), XY(1,boardSize.y-2), time, Bot.Wall)
        }




    def spawnRandomWalls(rnd: Random, board: Board, time: Time, boardSize: XY, count: Int) : Board = {
        var updatedBoard = board

        for(i <- 0 until count) {
            val wallType = rnd.nextInt(3)
            wallType match {
                case 0 =>       // line shape
                    val start = updatedBoard.randomPos(rnd, boardSize)
                    val length = rnd.nextInt(7) + 3
                    val step = if(rnd.nextBoolean()) XY(1,0) else XY(0,1)
                    updatedBoard = spawnWallPath(updatedBoard, boardSize, time, start,
                        Iterable((step, length)))

                case 1 =>       // "L" shape
                    val length = rnd.nextInt(4) + 2
                    val start = updatedBoard.randomPos(rnd, boardSize)
                    val dir = XY.randomDirection90(rnd)
                    updatedBoard = spawnWallPath(updatedBoard, boardSize, time, start,
                        Iterable(
                            (dir, length),
                            (dir.rotateCounterClockwise90, length)
                        ))

                case 2 =>       // "U" shape
                    val length = rnd.nextInt(3) + 4
                    val start = updatedBoard.randomPos(rnd, boardSize)
                    val dir = XY.randomDirection90(rnd)
                    updatedBoard = spawnWallPath(updatedBoard, boardSize, time, start,
                        Iterable(
                            (dir, length),
                            (dir.rotateCounterClockwise90, length),
                            (dir.rotateCounterClockwise90.rotateCounterClockwise90, length+1)
                        ))

                case _ => assert(false)
            }
        }

        updatedBoard
    }

    // path = Iterable[(step,count)]
    def spawnWallPath(board: Board, boardSize: XY, time: Time, start: XY, path: Iterable[(XY,Int)]) : Board = {
        var updatedBoard = board
        var pos = start
        path.foreach(pair => {
            val count = pair._2
            val step = pair._1
            require(step.stepCount == 1)

            // see how many wall cells we can cover without collision or hitting arena boundary
            val wallStartPos = pos
            var validCount = 0
            while(updatedBoard.isValidAndVacant(pos, boardSize) && validCount < count) {
                validCount += 1
                pos += step
            }


            if(step.y == 0 ) {
                // horizontal
                require(step.x != 0)

                // consolidate all cells into a single wall entity with larger extent
                if(validCount >= 1) {
                    // ensure positive extent
                    val fullStepX = step.x * validCount
                    if(fullStepX < 0) {
                        updatedBoard = updatedBoard.addBot(
                            XY(wallStartPos.x + fullStepX + 1, wallStartPos.y),
                            XY(-fullStepX, 1), time, Bot.Wall)
                    } else {
                        updatedBoard = updatedBoard.addBot(
                            XY(wallStartPos.x, wallStartPos.y),
                            XY(fullStepX, 1), time, Bot.Wall)
                    }
                }
            } else
            if(step.x == 0) {
                // vertical
                require(step.y != 0)

                // consolidate all cells into a single wall entity with larger extent
                if(validCount >= 1) {
                    // ensure positive extent
                    val fullStepY = step.y * validCount
                    if(fullStepY < 0) {
                        updatedBoard = updatedBoard.addBot(
                            XY(wallStartPos.x, wallStartPos.y + fullStepY + 1),
                            XY(1, -fullStepY), time, Bot.Wall)
                    } else {
                        updatedBoard = updatedBoard.addBot(
                            XY(wallStartPos.x, wallStartPos.y),
                            XY(1, fullStepY), time, Bot.Wall)
                    }
                }
            } else {
                // diagonal
                for(i <- 0 until validCount) {
                    updatedBoard = updatedBoard.addBot(pos + step*i, XY.One, time, Bot.Wall)
                }
            }

            if(validCount < count) return updatedBoard     // abort early!
        })
        updatedBoard
    }

}