// Example Bot #3: The Debug File Logger

import java.io.FileWriter

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction() {
    var pathAndRoundOpt : Option[(String,Int)] = None

    def respond(input: String): String = {
        val (opcode, paramMap) = CommandParser.apply(input)

        opcode match {
            case "Welcome" =>
                // first call made by the server.
                // We record the plug-in path and round index.
                val path = paramMap("path")
                val round = paramMap("round").toInt
                pathAndRoundOpt = Some((path,round))

            case "React" =>
                // called once per entity per simulation step.
                // We check the step index; if it is a multiple of
                // 100, we log our input into a file.
                val stepIndex = paramMap("time").toInt
                if((stepIndex % 100) == 0) {
                    val name = paramMap("name")
                    pathAndRoundOpt.foreach(pathAndRound => {
                        val (dirPath,roundIndex) = pathAndRound
                        val filePath = dirPath + "/" +
                            name + "_" +
                                       roundIndex + "_" +
                                       stepIndex + ".log"
                        val logFile = new FileWriter(filePath)

                        logFile.append(input)   // log the input

                        // if we logged more stuff, we might want an occasional newline:
                        logFile.append('\n')

                        // close the log file to flush what was written
                        logFile.close()
                    })
                }

            case "Goodbye" =>
                // last call made by the server. Nothing to do for us.

            case _ =>
                // plug-ins should simply ignore unknown command opcodes
        }
        ""      // return an empty string
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
