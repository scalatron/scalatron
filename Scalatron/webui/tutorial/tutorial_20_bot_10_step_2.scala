// Tutorial Bot #10: Food Finder
// Step 2: XY cell access

class ControlFunction {
    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val viewString = paramMap("view")
            val view = View(viewString)
            val cell = view(XY(1,1))
            "Status(text=cell at 1:1: " + cell + ")"
        } else ""
    }
}

case class View(cells: String) {
    val size = math.sqrt(cells.length).intValue
    def indexFromAbsPos(absPos: XY) = absPos.x + absPos.y * size
    def apply(absPos: XY) = cells.charAt(indexFromAbsPos(absPos))
}

case class XY(x: Int, y: Int)

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

