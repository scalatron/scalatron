/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.Font
import java.awt.Color

import RenderUtil.makeTransparent
import scalatron.botwar._

object GameStateRenderer {
    def drawGameState(state: State, drawBotHorizon: Boolean)(implicit ctx: RenderContext) {
        val (back, front) = state.board.decorations.values.partition(decoration =>
            decoration.variety.isInstanceOf[Decoration.Explosion] ||
                decoration.variety.isInstanceOf[Decoration.Bonk.type]
        )

        drawDecorations(state, back)
        drawShadowLayer(state, drawBotHorizon)
        drawObjectLayer(state)
        drawDecorations(state, front)
    }


    val ShadowColor = new Color(76, 75, 75)
    val BotViewColor = new Color(180, 180, 180, 25)

    def drawShadowLayer(state: State, drawBotHorizon: Boolean)(implicit ctx: RenderContext) {
        state.board.botsForEach(bot => {
            val (left, top) = ctx.leftTop(bot.pos)
            bot.variety match {
                case player: Bot.Player => // player shadow
                    if( player.isMaster ) {
                        if( drawBotHorizon ) {
                            val viewHalfSize = Constants.MasterHorizonHalfSize
                            ctx.setColor(BotViewColor)
                            ctx.fillRect(left - viewHalfSize * ctx.pixelsPerCell, top - viewHalfSize * ctx.pixelsPerCell, ( viewHalfSize * 2 + 1 ) * ctx.pixelsPerCell, ( viewHalfSize * 2 + 1 ) * ctx.pixelsPerCell)
                        }

                        val displacement = 4
                        ctx.setColor(ShadowColor)
                        ctx.fillOval(left - ctx.halfPixelsPerCell + displacement, top - ctx.halfPixelsPerCell + displacement, ctx.doublePixelsPerCell, ctx.doublePixelsPerCell)
                        ctx.setColor(Color.black)
                        ctx.fillOval(left - ctx.halfPixelsPerCell + displacement + 2, top - ctx.halfPixelsPerCell + displacement + 2, ctx.doublePixelsPerCell - 4, ctx.doublePixelsPerCell - 4)
                    } else {
                        val displacement = 4
                        ctx.setColor(ShadowColor)
                        ctx.fillOval(left + displacement, top + displacement, ctx.pixelsPerCell, ctx.pixelsPerCell)
                    }
                case Bot.Wall => // no shadow
                case Bot.BadPlant => // no shadow
                case _ => // beast / plant
                    val displacement = 4
                    ctx.setColor(ShadowColor)
                    ctx.fillRect(left + displacement, top + displacement, ctx.pixelsPerCell, ctx.pixelsPerCell)
            }
        })
    }


    val WallColorTriple = ColorTriple(new Color(55, 55, 55), new Color(84, 80, 80), new Color(123, 120, 120))
    val BadBeastColorTriple = ColorTriple(new Color(120, 0, 0), new Color(150, 0, 0), new Color(180, 80, 80))
    val GoodBeastColorTriple = ColorTriple(new Color(0, 0, 120), new Color(0, 0, 150), new Color(80, 80, 180))
    val BadPlantColorTriple = ColorTriple(new Color(120, 120, 0), new Color(150, 150, 0), new Color(180, 180, 80))
    val GoodPlantColorTriple = ColorTriple(new Color(0, 120, 0), new Color(0, 150, 0), new Color(0, 180, 0))

    val StatusBubbleFont = new Font("SansSerif", Font.PLAIN, 10)

    def drawObjectLayer(state: State)(implicit ctx: RenderContext) {
        val (walls, nonWalls) = state.board.botsThatAreWallsAndNonWalls

        def drawWalls() {
            ctx.setColor(WallColorTriple.bright)
            walls.foreach(bot => {
                val (left, top) = ctx.leftTop(bot.pos)
                val right = left + ctx.pixelsPerCell * bot.extent.x - 1
                val bottom = top + ctx.pixelsPerCell * bot.extent.y - 1
                ctx.drawLine(left - 1, top - 1, left - 1, bottom) // left
                ctx.drawLine(left - 1, top - 1, right, top - 1) // top
            })

            ctx.setColor(WallColorTriple.dark)
            walls.foreach(bot => {
                val (left, top) = ctx.leftTop(bot.pos)
                val right = left + ctx.pixelsPerCell * bot.extent.x // - 1
                val bottom = top + ctx.pixelsPerCell * bot.extent.y // - 1
                ctx.drawLine(left + 1, bottom, right, bottom) // bottom 1
                ctx.drawLine(right, top + 1, right, bottom - 1) // right 1
                ctx.drawLine(left + 2, bottom + 1, right + 1, bottom + 1) // bottom 2
                ctx.drawLine(right + 1, top + 2, right + 1, bottom) // right 2
            })

            ctx.setColor(WallColorTriple.plain)
            walls.foreach(bot => {
                val (left, top) = ctx.leftTop(bot.pos)
                ctx.fillRect(left, top, ctx.pixelsPerCell * bot.extent.x, ctx.pixelsPerCell * bot.extent.y)
            })
        }

        // walls first
        drawWalls()

        // then everyone else
        nonWalls.foreach(bot => {
            val (left, top) = ctx.leftTop(bot.pos)
            bot.variety match {
                case player: Bot.Player =>
                    if( player.isMaster ) {
                        val playerColorPair = Renderer.playerColors(bot.name)
                        ctx.drawMaster(left, top, playerColorPair, player.rankAndQuartile)
                        playerColorPair
                    } else {
                        val playerColorPair =
                            state.board.getBot(player.masterId) match {
                                case None => (WallColorTriple, WallColorTriple)
                                case Some(master) => Renderer.playerColors(master.name)
                            }
                        drawSlave(left, top, playerColorPair)
                        playerColorPair
                    }

                    player.stateMap.get(Protocol.PropertyName.Status) match {
                        case None => // OK - no status text
                        case Some(status) => // has status text - display it
                            val clippedStatus = if(status.length<=20) status else status.substring(0,20)
                            ctx.setColor(Color.white)
                            ctx.setFont(StatusBubbleFont)
                            ctx.drawString(clippedStatus, left + 15, top + 8)
                    }

                case Bot.BadBeast =>
                    ctx.drawBeveledRect(left, top, ctx.pixelsPerCell, ctx.pixelsPerCell, BadBeastColorTriple)
                case Bot.GoodBeast =>
                    ctx.drawBeveledRect(left, top, ctx.pixelsPerCell, ctx.pixelsPerCell, GoodBeastColorTriple)
                case Bot.BadPlant =>
                    ctx.drawEmbossedRect(left, top, ctx.pixelsPerCell, ctx.pixelsPerCell, BadPlantColorTriple)
                case Bot.GoodPlant =>
                    ctx.drawBeveledRect(left, top, ctx.pixelsPerCell, ctx.pixelsPerCell, GoodPlantColorTriple)
                case _ =>
                    assert(false)
            }
        })
    }

    def drawSlave(left: Int, top: Int, playerColorPair: (ColorTriple, ColorTriple))(implicit ctx: RenderContext) {
        val outerColor = playerColorPair._1
        ctx.setColor(outerColor.bright);
        ctx.fillOval(left - 1, top - 1, ctx.pixelsPerCell, ctx.pixelsPerCell)
        ctx.setColor(outerColor.dark);
        ctx.fillOval(left + 1, top + 1, ctx.pixelsPerCell, ctx.pixelsPerCell)
        ctx.setColor(outerColor.plain);
        ctx.fillOval(left, top, ctx.pixelsPerCell, ctx.pixelsPerCell)

        val innerColor = playerColorPair._2
        ctx.setColor(innerColor.plain);
        ctx.fillOval(left + ctx.halfPixelsPerCell/2, top + ctx.halfPixelsPerCell/2, ctx.halfPixelsPerCell, ctx.halfPixelsPerCell)
    }





    val ExplosionRingColor = new Color(255, 200, 0, 60)
    val ExplosionColor = Color.orange
    val BonkColor = new Color(180, 0, 0)

    val BonusFont = new Font("SansSerif", Font.PLAIN, 10)
    val BonusColor = Color.green
    val MalusColor = Color.red

    val TextDecorationFont = new Font("SansSerif", Font.PLAIN, 10)
    val TextColor = Color.white

    val AnnihilationColor = Color.magenta

    def drawDecorations(state: State, decorations: Iterable[Decoration])(implicit ctx: RenderContext) {
        decorations.foreach(decoration => {
            val (centerX, centerY) = ctx.center(decoration.pos)
            val age: Int = ( state.time - decoration.creationTime ).intValue
            val lifeFraction: Double = age.doubleValue / decoration.lifeTime
            val alpha: Int = ( 255 - 255 * lifeFraction ).intValue
            decoration.variety match {
                case explosion: Decoration.Explosion =>
                    val blastRadiusX = explosion.blastRadius * ctx.pixelsPerCell
                    val blastRadiusY = explosion.blastRadius * ctx.pixelsPerCell

                    ctx.setColor(ExplosionRingColor)
                    ctx.drawOval(centerX - blastRadiusX, centerY - blastRadiusY, blastRadiusX * 2, blastRadiusY * 2)

                    // explosion first grows, then shrinks
                    val radiusFactor = if( lifeFraction <= 0.25 ) lifeFraction * 4 else ( 1.0 - lifeFraction )
                    val radiusX = ( blastRadiusX * radiusFactor ).intValue
                    val radiusY = ( blastRadiusY * radiusFactor ).intValue
                    val color = makeTransparent(ExplosionColor, ( 255 * radiusFactor ).intValue)
                    ctx.setColor(color)
                    ctx.fillOval(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2)

                case Decoration.Bonk =>
                    val color = makeTransparent(BonkColor, alpha)
                    ctx.setColor(color)
                    ctx.drawOval(centerX - age / 2, centerY - age / 2, age, age)

                case bonus: Decoration.Bonus =>
                    val bonusEnergy = bonus.energy
                    val color = if( bonusEnergy > 0 ) makeTransparent(BonusColor, alpha) else makeTransparent(MalusColor, alpha)
                    ctx.setColor(color)
                    ctx.setFont(BonusFont)
                    ctx.drawString(bonusEnergy.toString, centerX + age, centerY + age)

                case text: Decoration.Text =>
                    val color = makeTransparent(TextColor, alpha)
                    ctx.setColor(color)
                    ctx.setFont(TextDecorationFont)
                    ctx.drawString(text.text, centerX, centerY)

                case Decoration.Annihilation =>
                    val color = makeTransparent(AnnihilationColor, alpha)
                    ctx.setColor(color)
                    ctx.drawOval(centerX - age, centerY - age, age * 2, age * 2)
                
                case markedCell: Decoration.MarkedCell =>
                    val color = makeTransparent(Color.decode(markedCell.color), alpha)
                    ctx.setColor(color)
                    val radius = ctx.pixelsPerCell / 2
                    ctx.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2)

                case line: Decoration.Line =>
                    val color = makeTransparent(Color.decode(line.color), alpha)
                    ctx.setColor(color)
                    val (toX, toY) = ctx.center(line.toPos)
                    ctx.drawLine(centerX, centerY, toX, toY)
                    
                case _ => // OK
            }
        })
    }
}

