/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.{Graphics2D, Color, Font}
import java.awt.image.BufferedImage

import scalatron.botwar.renderer.RenderUtil.makeTransparent
import scalatron.botwar.renderer.RenderContext.minBottomPanelHeight
import scalatron.botwar.renderer.RenderContext.minRightPanelWidth
import scalatron.botwar.Display.RenderTarget
import scalatron.botwar.{State, XY}


/** An instance of a Context object is used by the Renderer to maintain information about the
  * currently valid drawing parameters, such as canvas size and board size, as well as to
  * manage the image buffer used for off-screen drawing.
  */
object RenderContext {
    val minBottomPanelHeight = 26
    val minRightPanelWidth = 180
}
case class RenderContext(boardSize: XY, canvasSizeX: Int, canvasSizeY: Int)
{
    def isSuitableFor(renderTarget: RenderTarget, state: State) =
        renderTarget.canvasSizeX == canvasSizeX &&
            renderTarget.canvasSizeY == canvasSizeY &&
            state.config.boardParams.size == boardSize

    val maxFieldSizeX = canvasSizeX - minRightPanelWidth // e.g. 1024-150 = 874
    val maxFieldSizeY = canvasSizeY - minBottomPanelHeight // e.g. 768-26 = 742
    val maxPixelsPerCellX = ( maxFieldSizeX / boardSize.x ).intValue // e.g. 874/109 = 8.02 => 8
    val maxPixelsPerCellY = ( maxFieldSizeY / boardSize.y ).intValue // e.g. 742/92 = 8.07 => 8

    val pixelsPerCell = maxPixelsPerCellX.min(maxPixelsPerCellY).max(3) // e.g. 8

    val halfPixelsPerCell = pixelsPerCell / 2
    val doublePixelsPerCell = pixelsPerCell * 2

    def leftTop(pos: XY) = (pos.x * pixelsPerCell, pos.y * pixelsPerCell)

    def center(pos: XY) = (pos.x * pixelsPerCell + halfPixelsPerCell, pos.y * pixelsPerCell + halfPixelsPerCell)

    val fieldSizeX = pixelsPerCell * boardSize.x
    val fieldSizeY = pixelsPerCell * boardSize.y
    val fieldCenterX = fieldSizeX / 2
    val fieldCenterY = fieldSizeY / 2

    val rightPanelWidth = canvasSizeX - fieldSizeX
    val bottomPanelHeight = canvasSizeY - fieldSizeY

    val image = new BufferedImage(canvasSizeX, canvasSizeY, BufferedImage.TYPE_INT_ARGB)
    private val graphics = image.getGraphics // indicates the currently active graphics context for drawing

    def flipBuffer(frameGraphics: Graphics2D): Unit = {
        frameGraphics.drawImage(image, 0, 0, null)
    }

    def setColor(color: Color): Unit = {graphics.setColor(color)}

    def setFont(font: Font): Unit = {graphics.setFont(font)}

    def fillRect(x: Int, y: Int, width: Int, height: Int): Unit = {graphics.fillRect(x, y, width, height)}

    def drawRect(x: Int, y: Int, width: Int, height: Int): Unit = {graphics.drawRect(x, y, width, height)}

    def fillOval(x: Int, y: Int, width: Int, height: Int): Unit = {graphics.fillOval(x, y, width, height)}

    def drawOval(x: Int, y: Int, width: Int, height: Int): Unit = {graphics.drawOval(x, y, width, height)}

    def drawLine(x1: Int, y1: Int, x2: Int, y2: Int): Unit = {graphics.drawLine(x1, y1, x2, y2)}

    def drawString(s: String, x: Int, y: Int): Unit = {graphics.drawString(s, x, y)}

    def fillCenteredCircle(centerX: Int, centerY: Int, radius: Int): Unit = {fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2)}

    def drawCenteredCircle(centerX: Int, centerY: Int, radius: Int): Unit = {drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2)}


    def drawBeveledRect(left: Int, top: Int, width: Int, height: Int, colorTriple: ColorTriple): Unit = {
        val right = left + width - 1
        val bottom = top + height - 1

        setColor(colorTriple.plain)
        fillRect(left + 1, top + 1, width - 2, height - 2)

        setColor(colorTriple.bright)
        drawLine(left, top, left, bottom - 1) // left
        drawLine(left + 1, top, right - 1, top) // top

        setColor(colorTriple.dark)
        drawLine(left + 1, bottom, right, bottom) // bottom
        drawLine(right, top + 1, right, bottom - 1) // right
    }

    def drawEmbossedRect(left: Int, top: Int, width: Int, height: Int, colorTriple: ColorTriple): Unit = {
        val right = left + width - 1
        val bottom = top + height - 1

        setColor(colorTriple.plain)
        fillRect(left + 1, top + 1, width - 2, height - 2)

        setColor(colorTriple.dark)
        drawLine(left, top, left, bottom - 1) // left
        drawLine(left + 1, top, right - 1, top) // top

        setColor(colorTriple.bright)
        drawLine(left + 1, bottom, right, bottom) // bottom
        drawLine(right, top + 1, right, bottom - 1) // right
    }

    def drawEmbossedText(text: String, left: Int, top: Int, colorTriple: ColorTriple, alpha: Int): Unit = {
        setColor(makeTransparent(colorTriple.bright, alpha))
      drawString(text, left - 1, top - 1)
        setColor(makeTransparent(colorTriple.dark, alpha))
      drawString(text, left + 1, top + 1)
        setColor(makeTransparent(colorTriple.plain, alpha))
      drawString(text, left, top)
    }


    def drawMaster(left: Int, top: Int, playerColorPair: (ColorTriple, ColorTriple), rankAndQuartile: (Int, Int)): Unit = {
        val centerX = left + halfPixelsPerCell
        val centerY = top + halfPixelsPerCell

        // double-sized outer ring
        val outerColor = playerColorPair._1
        setColor(outerColor.bright)
        fillCenteredCircle(centerX - 1, centerY - 1, pixelsPerCell)
        setColor(outerColor.dark)
        fillCenteredCircle(centerX + 1, centerY + 1, pixelsPerCell)
        setColor(outerColor.plain)
        fillCenteredCircle(centerX, centerY, pixelsPerCell)

        // cell-sized inner square
        val innerColor = playerColorPair._2
        drawBeveledRect(left, top, pixelsPerCell, pixelsPerCell, innerColor)
    }
}


