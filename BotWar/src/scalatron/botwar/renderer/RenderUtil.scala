/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.Color


object RenderUtil
{
    def makeTransparent(color: Color, alpha: Int) = new Color(color.getRed, color.getGreen, color.getBlue, alpha)
}