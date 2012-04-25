// Example Bot #4: The Debug Status Logger

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction() {
    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser.apply(input)

        opcode match {
            case "React" =>
                "Log(text=" +
                    "time is " + paramMap("time") + "\n" +
                    "energy is " + paramMap("energy") + "\n" +
                    "generation is " + paramMap("generation") +
                    ")"

            case _ => ""
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
