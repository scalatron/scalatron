// Tutorial Bot 06 - Command Parser Object

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction {
    def respond(input: String) = {
        val (opcode, paramMap) = CommandParser(input)
        if(opcode=="React") {
            "Status(text=Energy:" + paramMap("energy") + ")"
        } else {
            ""
        }
    }
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
