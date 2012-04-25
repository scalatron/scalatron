/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.botPlugin.util

import java.awt.{Font, Color, Graphics}
import scalatron.botwar.XY



object DirectionValueRenderer {
    def render(view: View, directionValue: Array[Double], g: Graphics) {
        require(directionValue.length == 8)

        val cellSize = ViewRenderer.cellSize
        val bestDirection45 = directionValue.zipWithIndex.maxBy(_._1)._2
        val bestValue = directionValue(bestDirection45)
        val worstDirection45 = directionValue.zipWithIndex.minBy(_._1)._2
        val worstValue = directionValue(worstDirection45)

        for(x <- 0 until view.size) {
            for(y <- 0 until view.size) {
                val canvasPosX = x * cellSize
                val canvasPosY = y * cellSize

                val absPos = XY(x,y)
                val relPos = view.relPosFromAbsPos(absPos)
                if(relPos.isNonZero) {
                    val direction45 = relPos.toDirection45
                    val value = directionValue(direction45)

                    if(value < 0) {
                        val factor = value / worstValue
                        g.setColor(new Color((255*factor).intValue, 0, 0))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    } else
                    if(value > 0) {
                        val factor = value / bestValue
                        g.setColor(new Color(0, (255*factor).intValue, 0))
                        g.fillRect(canvasPosX, canvasPosY, cellSize, cellSize)
                    } else {

                    }
                }
            }
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 15))
        g.setColor(Color.white)
        for(direction45 <- 0 until 8) {
            val value = directionValue(direction45)
            val unitRelPos = XY.fromDirection45(direction45)
            val zoomedRelPos = unitRelPos * 10
            val absPos = view.absPosFromRelPos(zoomedRelPos)
            val canvasPosX = absPos.x * cellSize
            val canvasPosY = absPos.y * cellSize
            g.drawString(value.intValue.toString, canvasPosX, canvasPosY)
        }
    }
}
