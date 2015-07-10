/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration.Duration
import scalatron.botwar.Display.RenderTarget

import scalatron.botwar._
import java.awt.Color
import scalatron.core.{ScalatronInward, PermanentConfig}


object Renderer {
    /** A table of high-contrast colors that we'll use to assemble player color combinations. */
    val PrimaryColorTable = Array(
        Color.cyan,
        Color.red,
        Color.yellow,
        Color.green,
        Color.magenta
    )

    /** The same table, but as color triples (plain, lighter, darker). */
    val PrimaryColorTripleTable = PrimaryColorTable.map(color => ColorTriple(color))


    /** Given a player name, this function constructs two color triples that together should
      * provide a unique visual identification of a player's bot on the screen.
      */
    def playerColors(name: String) = {val hash = name.hashCode & 4095; (playerColorA(hash), playerColorB(hash))} // pair of primary colors
    def playerColorA(hash: Int) = PrimaryColorTripleTable(hash % PrimaryColorTripleTable.length)

    def playerColorB(hash: Int) = PrimaryColorTripleTable(( hash >>> 8 ) % PrimaryColorTripleTable.length)

    /*def printKeyboardCommands()*/ {
        println("Keyboard commands available in the display window:")
        println(" '1'   -- no delay between simulation steps")
        println(" '2'   -- delay = 50ms")
        println(" '3'   -- delay = 100ms")
        println(" '4'   -- delay = 250ms")
        println(" '5'   -- delay = 500ms")
        println(" '6'   -- delay = 1000ms")
        println(" '7'   -- delay = 2000ms")
        println(" space -- freeze/unfreeze the action")
        println(" 'h'   -- show/hide bot horizons")
        println(" 'r'   -- abort round, rescan for updated plug-ins and start next round")
        println(" '+/-' -- step through players")
    }
}


/** The renderer is responsible for rendering the state of a game into a graphics context.
  * @param permanentConfig the permanent game configuration
  * @param scalatron reference to the Scalatron API facing towards this game plug-in the (for access to current state of the tournament - contains e.g. leaderboard)
  */
case class Renderer(permanentConfig: PermanentConfig, scalatron: ScalatronInward) {
    val interactivelyAdjustableSettings = new InteractivelyAdjustableSettings

    def keyPressed(c: Char) {
        c match {
            case ' ' =>
                if( interactivelyAdjustableSettings.sleepTimeBetweenSteps == 0L )
                    interactivelyAdjustableSettings.sleepTimeBetweenSteps = Int.MaxValue /* not Long.MaxValue: overflow!*/
                else
                    interactivelyAdjustableSettings.sleepTimeBetweenSteps = 0L

            case '1' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 0L
            case '2' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 50L
            case '3' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 100L
            case '4' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 250L
            case '5' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 500L
            case '6' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 1000L
            case '7' => interactivelyAdjustableSettings.sleepTimeBetweenSteps = 2000L

            case 'h' => interactivelyAdjustableSettings.drawBotHorizon = !interactivelyAdjustableSettings.drawBotHorizon

            case 'r' => interactivelyAdjustableSettings.abortThisRound = true

            case '+' => interactivelyAdjustableSettings.nextPlayerOfInterest()
            case '-' => interactivelyAdjustableSettings.prevPlayerOfInterest()

            case _ => // OK
        }
    }



    var pendingRenderJobs : List[RenderJob] = Nil
    var availableRenderContexts : List[RenderContext] = Nil

    def draw(renderTarget: RenderTarget, state: State)(implicit executionContextForTrustedCode: ExecutionContext)
    {
        if( state.time <= 1 ) {
            interactivelyAdjustableSettings.introStartTime = System.currentTimeMillis
            interactivelyAdjustableSettings.abortThisRound = false
        }

        // eliminate no-longer-suitable render contexts before recycling them
        availableRenderContexts = availableRenderContexts.filter(_.isSuitableFor(renderTarget, state))

        // recycle or create a render context for the job representing the new first pipeline state
        val renderContext =
            if(availableRenderContexts.isEmpty) {
                RenderContext(state.config.boardParams.size, renderTarget.canvasSizeX, renderTarget.canvasSizeY)
            } else {
                val contextToRecycle = availableRenderContexts.head
                availableRenderContexts = availableRenderContexts.tail // remove it from the list of available ones
                contextToRecycle
            }

        val sourceAndTarget = RenderSourceAndTarget(state, scalatron, interactivelyAdjustableSettings, permanentConfig, renderContext, renderTarget.frameGraphics)
        val newRenderJob = RenderJob(RenderStage.ClearBackground, sourceAndTarget)
        val renderJobs = newRenderJob :: pendingRenderJobs

        // start all render jobs concurrently
        val futureList = Future.traverse(renderJobs)(job => Future { job.renderStage(job.sourceAndTarget) })

        // wait until all render jobs finish; each returns an Option with a new render job
        val resultList = Await.result(futureList, Duration.Inf)     // no timeout; maybe in the future

        // now eliminate all completed jobs and recycle their render contexts
        val (remainingJobs, recyclableContexts) = resultList.partition(_.isLeft)
        pendingRenderJobs = remainingJobs.map(_.left.get)
        availableRenderContexts :::= recyclableContexts.map(_.right.get)


        val startTime = System.currentTimeMillis
        while( startTime + interactivelyAdjustableSettings.sleepTimeBetweenSteps > System.currentTimeMillis ) {
            Thread.sleep(10)
        }
    }
}

