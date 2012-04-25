// Tutorial Bot #9: Missile Launcher II
// Step 5(b): introducing an apply() method

import util.Random

class ControlFunction {
    val rnd = new Random()

    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser(input)
        if( opcode == "React" ) {
            val generation = paramMap("generation").toInt
            if( generation == 0 ) {
                if( paramMap("energy").toInt >= 100 && rnd.nextDouble() < 0.05 ) {
                    val xy = XY.random(rnd)
                    val direction = xy.x + ":" + xy.y // e.g. "-1:1"
                    "Spawn(direction=" + direction + ",heading=" + direction + ")"
                } else ""
            } else {
                val heading = paramMap("heading")
                "Move(direction=" + heading + ")"
            }
        } else ""
    }
}

class XY(val x: Int, val y: Int) {
    def +(other: XY) = XY(x+other.x, y+other.y)
}
object XY {
    def apply(x: Int, y: Int) = new XY(x, y)
    def random(rnd: Random) = XY(rnd.nextInt(3)-1, rnd.nextInt(3)-1)
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

