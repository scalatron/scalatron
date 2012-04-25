/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import java.awt.geom.AffineTransform
import java.awt.{Toolkit, Frame, Graphics2D}
import scalatron.botwar.Display.RenderTarget
import scalatron.Version


object Display {
    def create(argMap: Map[String, String]) = {
        def createFrame(windowSizeX: Int, windowSizeY: Int, title: String) = {
            val frame = new Frame(title)
            frame.addNotify()                           // connect frame to native screen resource

            // handle insets (window frame, title bar, etc.)
            frame.setSize( windowSizeX, windowSizeY )

            frame.setExtendedState(frame.getExtendedState & Frame.MAXIMIZED_BOTH)

            frame.setVisible( true )
            frame
        }

        val defaultWindowSizeX = 640
        val defaultWindowSizeY = 500
        val toolkit = Toolkit.getDefaultToolkit
        val screenSize = toolkit.getScreenSize

        val windowSizeX = argMap.get("-frameX") match {
            case None => defaultWindowSizeX
            case Some(s) =>
                if(s.toLowerCase=="max") screenSize.getWidth.intValue else s.toInt
        }
        val windowSizeY = argMap.get("-frameY") match {
            case None => defaultWindowSizeY
            case Some(s) => if(s.toLowerCase=="max") screenSize.getHeight.intValue else s.toInt
        }

        val frame = createFrame(windowSizeX, windowSizeY, "Scalatron " + Constants.GameName + " " + Version.VersionString)

        Display(frame)
    }

    /** Dump the display-specific command line configuration options via println. */
    def printArgList() {
        println("  -frameX <int>|max        window width (pixels; default: 640)")
        println("  -frameY <int>|max        window height (pixels; default: 500)")
    }

    case class RenderTarget(frameGraphics: Graphics2D, canvasSizeX: Int, canvasSizeY: Int)
}

case class Display(frame: Frame) {
    def show() { frame.setVisible(true) }
    def hide() { frame.setVisible(false)}

    def renderTarget: RenderTarget = {
        val frameGraphics = frame.getGraphics.asInstanceOf[Graphics2D]
        val insets = frame.getInsets
        frameGraphics.setTransform( AffineTransform.getTranslateInstance(insets.left, insets.top) )
        val canvasSizeX = frame.getWidth - insets.left - insets.right
        val canvasSizeY = frame.getHeight - insets.top - insets.bottom
        RenderTarget(frameGraphics, canvasSizeX, canvasSizeY)
    }
}
