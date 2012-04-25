/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.botPlugin.util

import scalatron.botwar.XY



case class View(cells: String) {
    val size = math.sqrt(cells.length).toInt
    val center = XY(size/2, size/2)

    def apply(relPos: XY) = cellAtRelPos(relPos)

    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
    def absPosFromIndex(index: Int) = XY(index % size, index / size)
    def absPosFromRelPos(relPos: XY) = relPos + center
    def cellAtAbsPos(absPos: XY) = cells.charAt(indexFromAbsPos(absPos))

    def indexFromRelPos(relPos: XY) = indexFromAbsPos(absPosFromRelPos(relPos))
    def relPosFromAbsPos(absPos: XY) = absPos - center
    def relPosFromIndex(index: Int) = relPosFromAbsPos(absPosFromIndex(index))
    def cellAtRelPos(relPos: XY) = cells.charAt(indexFromRelPos(relPos))

    // while()
    def offsetToNearest2(c: Char) = {
        var nearestPosOpt : Option[XY] = None
        var nearestDistance = Double.MaxValue
        var i = 0
        val cellCount = cells.length
        while(i < cellCount) {
            if(cells(i) == c) {
                val pos = absPosFromIndex(i)
                val distanceToCenter = pos.distanceTo(center)
                if(distanceToCenter < nearestDistance) {
                    nearestDistance = distanceToCenter
                    nearestPosOpt = Some(pos - center)
                }
            }
            i += 1
        }
        nearestPosOpt
    }

    // for()
    def offsetToNearest1(c: Char) = {
        var nearestPosOpt : Option[XY] = None
        var nearestDistance = Double.MaxValue
        for(i <- 0 until cells.length) {
            if(c == cells(i)) {
                val pos = absPosFromIndex(i)
                val distanceToCenter = pos.distanceTo(center)
                if(distanceToCenter < nearestDistance) {
                    nearestDistance = distanceToCenter
                    nearestPosOpt = Some(pos - center)
                }
            }
        }
        nearestPosOpt
    }

    // zipWithIndex()
    def offsetToNearest(c: Char) = {
        val matchingXY = cells.view.zipWithIndex.filter(_._1 == c)
        if(matchingXY.isEmpty)
            None
        else {
            val nearest = matchingXY.map(p => relPosFromIndex(p._2)).minBy(_.length)
            Some(nearest)
        }
    }
}
