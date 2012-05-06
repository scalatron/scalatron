/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.{Font, Color}
import scalatron.botwar.{PermanentConfig, State}
import scalatron.game.TournamentState


object StatsPanelRenderer
{
    val StatsFont = new Font("SansSerif", Font.PLAIN, 10)
    var lastDrawTime = System.currentTimeMillis
    val BgColorTriple = ColorTriple(new Color(105, 105, 105), new Color(123, 120, 120), new Color(145, 145, 145))
    val frameTimeQueue = scala.collection.mutable.Queue.empty[Long]

    def draw(state: State, tournamentState: TournamentState, permanentConfig: PermanentConfig, interactivelyAdjustableSettings: InteractivelyAdjustableSettings)(implicit ctx: RenderContext) {
        ctx.setColor(BgColorTriple.plain)
        ctx.fillRect(0, ctx.fieldSizeY + 1, ctx.fieldSizeX + 1, ctx.bottomPanelHeight)

        // Stats & Time
        ctx.setFont(StatsFont)
        ctx.setColor(Color.white)
        val board = state.board
        val y = ctx.fieldSizeY + 15

        val currentTime = System.currentTimeMillis
        val gameTime = currentTime - interactivelyAdjustableSettings.introStartTime
        val frameTime = currentTime - lastDrawTime
        lastDrawTime = currentTime

        frameTimeQueue.enqueue(frameTime)
        if( frameTimeQueue.size >= 20 ) frameTimeQueue.dequeue()
        val avgFrameTime = frameTimeQueue.sum / frameTimeQueue.size

        ctx.drawString(
            "round " + ( tournamentState.roundsPlayed + 1 ) + ": " + state.time + " steps (of " + permanentConfig.stepsPerRound + "), " +
            ( gameTime / 1000.0 ).formatted("%.1f") + "s, " + avgFrameTime + "ms/step", 10, y)

        ctx.drawString(board.botCount + " bots, " + board.decorations.size + " decorations", 280, y)
        if( interactivelyAdjustableSettings.sleepTimeBetweenSteps > 0 ) ctx.drawString("[sleep " + interactivelyAdjustableSettings.sleepTimeBetweenSteps + "ms]", 520, y)
    }

}