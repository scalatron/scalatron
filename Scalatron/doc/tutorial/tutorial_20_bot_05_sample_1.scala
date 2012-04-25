// Tutorial Bot #5: Command Parser Function

class ControlFunction {
    def respond(input: String) = {
        val parseResult = parse(input)
        val opcode = parseResult._1
        val paramMap = parseResult._2
        if(opcode=="React") {
            "Status(text=Energy:" + paramMap("energy") + ")"
        } else {
            ""
        }
    }

    def parse(command: String) = {
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

