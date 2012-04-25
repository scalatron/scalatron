/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.botPlugin.util

import java.awt.{Color, Font, Graphics}
import scalatron.botwar.XY



object ViewRenderer {
    val cellSize = 10
    val expectedCellCount = 33
    val canvasSize = expectedCellCount * cellSize

    def render(view: View, g: Graphics) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10))
        for(x <- 0 until view.size) {
            for(y <- 0 until view.size) {
                val absPos = XY(x,y)
                val cellChar = view.cellAtAbsPos(absPos)
                val canvasPosX = x * cellSize
                val canvasPosY = y * cellSize

                cellChar match {
                    case 'B' =>
                        g.setColor(new Color(0,0,160))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    case 'b' =>
                        g.setColor(new Color(160,0,0))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    case 'P' =>
                        g.setColor(new Color(0,160,0))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    case 'p' =>
                        g.setColor(new Color(160,160,0))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    case 'W' =>
                        g.setColor(new Color( 94, 90, 90))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    case '_' =>
                    case _ =>
                        g.setColor(Color.white)
                        g.drawString(cellChar.toString, canvasPosX, canvasPosY+cellSize)
                }
            }
        }
    }
}
