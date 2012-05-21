/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer



import java.awt.{Font, Color}
import scalatron.core.TournamentState


object LeaderboardPanelRenderer
{
    val FontSize = 10
    val StatsFont = new Font("SansSerif", Font.PLAIN, FontSize)
    val BgColorTriple = ColorTriple(new Color(105, 105, 105), new Color(123, 120, 120), new Color(145, 145, 145))
    val TextColor = new Color(200, 200, 200)
    val NameColor = new Color(200, 200, 255)

    val LineHeight = FontSize + 5
    val panelHeight = 4 * LineHeight + 2 * panelInnerMargin + 5
    val panelOuterMargin = 2
    val panelInnerMargin = 4

    def draw(leaderBoard: TournamentState.LeaderBoard)(implicit ctx: RenderContext) {
        val panelOuterTop = ctx.canvasSizeY - panelHeight
        val panelOuterLeft = ctx.fieldSizeX + panelOuterMargin
        ctx.drawBeveledRect(panelOuterLeft, panelOuterTop, ctx.rightPanelWidth - 2 * panelOuterMargin, panelHeight, BgColorTriple)

        val panelInnerLeft = panelOuterLeft + panelInnerMargin
        val panelInnerTop = panelOuterTop + panelInnerMargin

        // Stats & Time
        ctx.setFont(StatsFont)

        ctx.setColor(TextColor)
        val y0 = panelInnerTop + FontSize
        val x1 = panelInnerLeft
        ctx.drawString("last 1: ",   x1, y0 + 0 * LineHeight)
        ctx.drawString("last 5: ",   x1, y0 + 1 * LineHeight)
        ctx.drawString("last 20: ",  x1, y0 + 2 * LineHeight)
        ctx.drawString("all time: ", x1, y0 + 3 * LineHeight)

        ctx.setColor(NameColor)
        val x2 = panelInnerLeft + 50
        for(i <- 0 until leaderBoard.length) {
            ctx.drawString(leaderBoard(i)._2.take(3).map(t => t._1 + ":" + t._2).mkString(", "), x2, y0 + i * LineHeight)
        }
    }
}