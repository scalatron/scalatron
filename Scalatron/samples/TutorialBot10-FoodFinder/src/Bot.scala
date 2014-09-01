// Tutorial Bot 10 - Food Finder

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction {
    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)

        if( opcode == "React" ) {
            val viewString = paramMap("view")
            val view = View(viewString)
            view.offsetToNearest('P') match {
                case Some(offset) =>
                    val unitOffset = offset.signum
                    "Move(direction=" + unitOffset + ")|Status(text=Harvesting)"
                case None =>
                    "Status(text=No Food)"
            }
        } else ""
    }
}


case class View(cells: String) {
    val size = math.sqrt(cells.length).toInt
    val center = XY(size/2, size/2)

    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
    def absPosFromIndex(index: Int) = XY(index % size, index / size)
    def absPosFromRelPos(relPos: XY) = relPos + center
    def cellAtAbsPos(absPos: XY) = cells.charAt(indexFromAbsPos(absPos))

    def indexFromRelPos(relPos: XY) = indexFromAbsPos(absPosFromRelPos(relPos))
    def relPosFromAbsPos(absPos: XY) = absPos - center
    def relPosFromIndex(index: Int) = relPosFromAbsPos(absPosFromIndex(index))
    def cellAtRelPos(relPos: XY) = cells.charAt(indexFromRelPos(relPos))

    def offsetToNearest(c: Char) = {
        val relativePositions = cells.par.view.zipWithIndex.filter(_._1 == c).map(p => relPosFromIndex(p._2))
        if(relativePositions.isEmpty) None
        else Some(relativePositions.minBy(_.length))
    }
}


case class XY(x: Int, y: Int) {
    override def toString = x + ":" + y
    def +(pos: XY) = XY(x+pos.x, y+pos.y)
    def -(pos: XY) = XY(x-pos.x, y-pos.y)
    def distanceTo(pos: XY) : Double = (this-pos).length
    def length : Double = math.sqrt(x*x + y*y)
    def signum = XY(x.signum, y.signum)
    def toEntityName = x + "_" + y
}


object CommandParser {
    def apply(command: String) = {
        def splitParam(param: String) = {
            val segments = param.split('=')
            if( segments.length != 2 )
                throw new IllegalStateException("invalid key/value pair: " + param)
            (segments(0),segments(1))
        }

        val segments = command.split('(')
        if( segments.length != 2 )
            throw new IllegalStateException("invalid command: " + command)

        val params = segments(1).dropRight(1).split(',')
        val keyValuePairs = params.map( splitParam ).toMap
        (segments(0), keyValuePairs)
    }
}
