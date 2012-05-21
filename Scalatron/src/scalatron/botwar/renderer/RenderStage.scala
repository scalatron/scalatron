package scalatron.botwar.renderer

import scalatron.scalatron.impl.TournamentState
import scalatron.botwar.{PermanentConfig, State}
import java.awt.{Font, Color, Graphics2D}


case class RenderSourceAndTarget(
    state: State,
    tournamentState: TournamentState,
    interactivelyAdjustableSettings: InteractivelyAdjustableSettings,
    permanentConfig: PermanentConfig,
    renderContext: RenderContext,
    frameGraphics: Graphics2D)
case class RenderJob(renderStage: RenderStage, sourceAndTarget: RenderSourceAndTarget)


/** A render stage is a processing stage for a render job, such as clearing, painting and blitting.
  * All render stage instances maintained by the renderer will execute concurrently.
  * More stages means more parallelism but higher latency and more memory consumption. Since latency is not an
  * issue in the context of Scalatron (it's all bots, they don't mind) and memory consumption is moderate, more
  * stages (within reason) and thus more parallelism are better.
  *
  * Each state receives a render source (game state) and target (rendering context) and returns either
  * a new render job that will operate on the same target or, if the pipeline is done, a to-be-recycled
  * RenderContext.
  */
sealed trait RenderStage {
    def apply(sourceAndTarget: RenderSourceAndTarget) : Either[RenderJob,RenderContext]
}
object RenderStage {
    case object ClearBackground extends RenderStage {
        val FieldColorTriple = ColorTriple(new Color(80, 80, 80), new Color(103, 100, 100), new Color(112, 110, 110))

        def apply(sourceAndTarget: RenderSourceAndTarget) = {
            val ctx = sourceAndTarget.renderContext

            // background
            ctx.setColor(FieldColorTriple.plain)
            ctx.fillRect(0, 0, ctx.fieldSizeX + 1, ctx.fieldSizeY + 1)

            val cellsBetweenGridLines = 10
            val gridSpacingX = cellsBetweenGridLines * ctx.pixelsPerCell
            var x = gridSpacingX
            while( x < ctx.fieldSizeX ) {
                ctx.setColor(FieldColorTriple.dark)
                ctx.drawLine(x, 0, x, ctx.fieldSizeY)
                ctx.setColor(FieldColorTriple.bright)
                ctx.drawLine(x + 1, 0, x + 1, ctx.fieldSizeY)
                x += gridSpacingX
            }

            val gridSpacingY = cellsBetweenGridLines * ctx.pixelsPerCell
            var y = gridSpacingY
            while( y < ctx.fieldSizeY ) {
                ctx.setColor(FieldColorTriple.dark)
                ctx.drawLine(0, y, ctx.fieldSizeX, y)
                ctx.setColor(FieldColorTriple.bright)
                ctx.drawLine(0, y + 1, ctx.fieldSizeX, y + 1)
                y += gridSpacingY
            }

            Left(RenderJob(DrawGameState, sourceAndTarget))
        }
    }

    case object DrawGameState extends RenderStage {
        def apply(sourceAndTarget: RenderSourceAndTarget) = {
            GameStateRenderer.drawGameState(
                sourceAndTarget.state,
                sourceAndTarget.interactivelyAdjustableSettings.drawBotHorizon
            )(sourceAndTarget.renderContext)
            Left(RenderJob(DrawStatsAndLeaderboardPanel, sourceAndTarget))
        }
    }

    case object DrawStatsAndLeaderboardPanel extends RenderStage {
        def apply(sourceAndTarget: RenderSourceAndTarget) = {
            implicit val ctx = sourceAndTarget.renderContext

            StatsPanelRenderer.draw(
                sourceAndTarget.state,
                sourceAndTarget.tournamentState,
                sourceAndTarget.permanentConfig,
                sourceAndTarget.interactivelyAdjustableSettings )


            ScorePanelRenderer.draw(sourceAndTarget.state, sourceAndTarget.interactivelyAdjustableSettings)

            LeaderboardPanelRenderer.draw(sourceAndTarget.tournamentState.leaderBoard)

            Left(RenderJob(BlitToScreen, sourceAndTarget))
        }
    }
    case object BlitToScreen extends RenderStage {
        def apply(sourceAndTarget: RenderSourceAndTarget) = {
            val renderContext = sourceAndTarget.renderContext
            renderContext.flipBuffer(sourceAndTarget.frameGraphics)
            Right(renderContext)
        }
    }
}

