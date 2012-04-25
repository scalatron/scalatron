/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.renderer

import java.awt.Color


/** A container class that holds a 'plain' color as well as a darker and lighter variant of it.
  * The lighter and darker variants are used to provide a bit of an embossed / pseudo-3D look
  * for on-screen elements. */
case class ColorTriple(dark: Color, plain: Color, bright: Color)

object ColorTriple {
    def apply(color: Color): ColorTriple = ColorTriple(color.darker, color, color.brighter)
}
