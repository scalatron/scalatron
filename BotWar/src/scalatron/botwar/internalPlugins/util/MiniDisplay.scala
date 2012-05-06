/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.botPlugin.util

import java.awt.image.BufferedImage
import java.awt.{Graphics2D, Frame, Color, Graphics}
import java.awt.geom.AffineTransform


/** An extremely simple debug view that a bot can pop up to display its input and internal state. */
class MiniDisplay(canvasSizeX: Int, canvasSizeY: Int, posX: Int, posY: Int, title: String) {
    private val (frame, frameGraphics) = createFrame(canvasSizeX, canvasSizeY, posX, posY, title)
    private val image = new BufferedImage(canvasSizeX, canvasSizeY, BufferedImage.TYPE_INT_ARGB )
    private val g = image.getGraphics

    def getGraphics: Graphics = g
    def blitToScreen() { frameGraphics.drawImage( image, 0, 0, null ) }
    def clear() { g.setColor(Color.darkGray); g.fillRect(0,0,canvasSizeX,canvasSizeY) }

    private def createFrame(sizeX: Int, sizeY: Int, posX: Int, posY: Int, title: String) = {
        val frame = new Frame(title)
        frame.addNotify()                           // connect frame to native screen resource

        // handle insets (window frame, title bar, etc.)
        val insets = frame.getInsets
        val windowWidth = sizeX + insets.left + insets.right
        val windowHeight = sizeY + insets.top + insets.bottom
        frame.setSize(windowWidth, windowHeight)
        frame.setLocation(posX, posY)

        frame.setVisible( true )

        val frameGraphics = frame.getGraphics.asInstanceOf[Graphics2D]
        frameGraphics.setTransform( AffineTransform.getTranslateInstance(insets.left, insets.top) )
        (frame,frameGraphics)
    }
}