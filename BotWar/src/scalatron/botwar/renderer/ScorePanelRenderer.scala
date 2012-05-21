/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.{Color, Font}
import scalatron.botwar.Bot
import scalatron.botwar.State


/** Maintains the "player of interest" indicator and handles the drawing of the score display
  * panel on the top-right of the window. */
object ScorePanelRenderer
{
    val ScoreFontSize = 10
    val ScoreFont = new Font("SansSerif", Font.PLAIN, ScoreFontSize)
    val ScoreBoardColor = Color.white
    val ScoreBarColorTriple = ColorTriple(new Color(170, 170, 250))
    val ScorePanelColorTriple = ColorTriple(new Color(105, 105, 105), new Color(123, 120, 120), new Color(145, 145, 145))
    val ScorePanelHiliteColorTriple = ColorTriple(new Color(125, 125, 125), new Color(143, 140, 140), new Color(165, 165, 165))

    def draw(state: State, interactivelyAdjustableSettings: InteractivelyAdjustableSettings)(implicit ctx: RenderContext) {
        ctx.setFont(ScoreFont)
        ctx.setColor(ScorePanelColorTriple.plain)
        ctx.fillRect(ctx.fieldSizeX + 1, 0, ctx.rightPanelWidth, ctx.canvasSizeY)

        val rankedPlayers = state.rankedPlayers
        val rankedPlayerCount = rankedPlayers.length

        // handle the case where the user stepped the POI beyond the last player -> clear it
        interactivelyAdjustableSettings.playerOfInterestOpt match {
            case None => false
            case Some((rankOfInterest, nameOfInterest)) =>
                if( rankOfInterest >= rankedPlayerCount ) interactivelyAdjustableSettings.playerOfInterestOpt = None
        }


        if( rankedPlayerCount >= 1 ) {
            val topScore = rankedPlayers.head.energy.doubleValue
            val left = ctx.fieldSizeX
            val panelOuterMargin = 2
            val panelInnerMargin = 4
            val panelOuterLeft = left + panelOuterMargin
            val panelInnerLeft = panelOuterLeft + panelInnerMargin

            val scoreBarHeight = 10
            val playerIconSize = ctx.doublePixelsPerCell
            val upperBlockHeight = panelInnerMargin * 2 + playerIconSize.max(ScoreFontSize * 2 + 2)
            val lowerBlockHeight = panelInnerMargin * 2 + scoreBarHeight
            val scorePanelHeight = upperBlockHeight + lowerBlockHeight


            def drawScorePanel(bot: Bot) {
                bot.variety match {
                    case player: Bot.Player =>
                        val rankAndQuartile = player.rankAndQuartile
                        val rank = rankAndQuartile._1
                        val quartile = rankAndQuartile._2

                        val playerName = bot.name
                        val playerColorPair = Renderer.playerColors(playerName)

                        val panelOuterTop = ( panelOuterMargin + scorePanelHeight ) * rank
                        val panelUpperTop = panelOuterTop + panelInnerMargin

                        // highlight the bot
                        val thisIsTheBotOfInterest =
                            interactivelyAdjustableSettings.playerOfInterestOpt match {
                                case None => false
                                case Some((rankOfInterest, nameOfInterest)) =>
                                    // the keyboard handler does not know about the actual names and ranks,
                                    // so we need to complete the information here. If a name is known,
                                    // it takes precedence over the rank.
                                    if( nameOfInterest == "" && rankOfInterest == rank ) {
                                        // name is not known; user selected a new rank => update the name
                                        interactivelyAdjustableSettings.playerOfInterestOpt = Some((rank, playerName))
                                        true
                                    } else if( nameOfInterest == playerName ) {
                                        // name is known => update the rank
                                        interactivelyAdjustableSettings.playerOfInterestOpt = Some((rank, nameOfInterest))
                                        true
                                    } else {
                                        false
                                    }
                            }

                        // panel
                        if( thisIsTheBotOfInterest ) {
                            val (centerX, centerY) = ctx.center(bot.pos)

                            // line from panel to bot
                            ctx.setColor(Color.white)
                            ctx.drawLine(panelOuterLeft, panelOuterTop + scorePanelHeight / 2, centerX, centerY)

                            ctx.drawBeveledRect(panelOuterLeft, panelOuterTop, ctx.rightPanelWidth - panelOuterMargin * 2, scorePanelHeight, ScorePanelHiliteColorTriple)
                        } else {
                            ctx.drawBeveledRect(panelOuterLeft, panelOuterTop, ctx.rightPanelWidth - panelOuterMargin * 2, scorePanelHeight, ScorePanelColorTriple)
                        }

                        // player bot icon
                        ctx.drawMaster(panelInnerLeft + ctx.halfPixelsPerCell, panelUpperTop + ctx.halfPixelsPerCell, playerColorPair, rankAndQuartile)

                        // score bar
                        val score = bot.energy
                        if( score > 0 ) {
                            val panelLowerTop = panelOuterTop + upperBlockHeight + panelInnerMargin
                            val relativeScore = score / topScore
                            val barLength = ( ( ctx.rightPanelWidth - 10 ) * relativeScore ).intValue
                            ctx.drawBeveledRect(panelInnerLeft, panelLowerTop, barLength, scoreBarHeight, ScoreBarColorTriple) // playerColorPair._1)
                        }

                        // name & score
                        ctx.setColor(Color.white)
                        val infoLeft = panelInnerLeft + playerIconSize + 6
                        ctx.drawString(playerName, infoLeft, panelUpperTop + 10)
                        ctx.drawString(score.toString, infoLeft, panelUpperTop + 23)

                        // rank & quartile
                        ctx.setColor(Color.black)
                        val infoRight = ctx.canvasSizeX - panelOuterMargin - panelInnerMargin - 30
                        ctx.drawString("#" + ( rank + 1 ), infoRight, panelUpperTop + 10)
                        def quartileName(quartile: Int) = quartile match {
                            case 0 => "1st"
                            case 1 => "2nd"
                            case 2 => "3rd"
                            case _ => "4th"
                        }
                        ctx.drawString(quartileName(quartile) + " Q", infoRight, panelUpperTop + 23)

                        // CPU time
                        val infoMid = infoRight - 40
                        val timeString = ( player.cpuTime.doubleValue / state.time / 1e6 ).formatted("%.1fms")
                        ctx.drawString(timeString, infoMid, panelUpperTop + 10)

                    case _ => assert(false)
                }
            }

            rankedPlayers.foreach(drawScorePanel)
        }
    }
}