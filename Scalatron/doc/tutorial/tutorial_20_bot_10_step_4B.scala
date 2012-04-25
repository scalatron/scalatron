// Tutorial Bot #10: Food Finder
// Step 4: Variant B: functional style

class ControlFunction {
    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val viewString = paramMap("view")
            val view = View(viewString)
            view.offsetToNearest('P') match {
                case None => "Status(text=no edible plant nearby)"
                case Some(offset) => "Status(text=edible plant at " + offset + ")"
            }
        } else ""
    }
}

case class View(cells: String) {
    val size = math.sqrt(cells.length).toInt
    val center = XY(size/2, size/2)

    def offsetToNearest(c: Char) = {
        val relativePositions =
            cells
            .view
            .zipWithIndex
            .filter(_._1 == c)
            .map(p => relPosFromIndex(p._2))
        if(relativePositions.isEmpty)
            None
        else
            Some(relativePositions.minBy(_.length))
    }

    def apply(relPos: XY) = cellAtRelPos(relPos)

    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
    def absPosFromIndex(index: Int) = XY(index % size, index / size)
    def absPosFromRelPos(relPos: XY) = relPos + center
    def cellAtAbsPos(absPos: XY) = cells.apply(indexFromAbsPos(absPos))

    def indexFromRelPos(relPos: XY) = indexFromAbsPos(absPosFromRelPos(relPos))
    def relPosFromAbsPos(absPos: XY) = absPos - center
    def relPosFromIndex(index: Int) = relPosFromAbsPos(absPosFromIndex(index))
    def cellAtRelPos(relPos: XY) = cells(indexFromRelPos(relPos))
}

case class XY(x: Int, y: Int) {
    override def toString = x + ":" + y

    def isNonZero = x!=0 || y!=0
    def isZero = x==0 && y==0
    def isNonNegative = x>=0 && y>=0

    def updateX(newX: Int) = XY(newX, y)
    def updateY(newY: Int) = XY(x, newY)

    def addToX(dx: Int) = XY(x+dx, y)
    def addToY(dy: Int) = XY(x, y+dy)

    def +(pos: XY) = XY(x+pos.x, y+pos.y)
    def -(pos: XY) = XY(x-pos.x, y-pos.y)
    def *(factor: Double) = XY((x*factor).intValue, (y*factor).intValue)

    def distanceTo(pos: XY) : Double = (this-pos).length
    def length : Double = math.sqrt(x*x + y*y)

    def signum = XY(x.signum, y.signum)

    def negate = XY(-x, -y)
    def negateX = XY(-x, y)
    def negateY = XY(x, -y)

}
object XY {
    def apply(s: String) : XY = {
        val xy = s.split(':').map(_.toInt) // e.g. "-1:1" => Array(-1,1)
        XY(xy(0), xy(1))
    }

    val Zero = XY(0,0)
    val One =  XY(1,1)

    val Right      = XY( 1,  0)
    val RightUp    = XY( 1, -1)
    val Up         = XY( 0, -1)
    val UpLeft     = XY(-1, -1)
    val Left       = XY(-1,  0)
    val LeftDown   = XY(-1,  1)
    val Down       = XY( 0,  1)
    val DownRight  = XY( 1,  1)
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

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

