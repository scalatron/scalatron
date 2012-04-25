// Tutorial Bot 11 - Handler Methods

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction() {
    // this method is called by the server
    def respond(input: String): String = {
        val (opcode, params) = CommandParser(input)
        opcode match {
            case "Welcome" =>
                welcome(
                    params("name"),
                    params("path"),
                    params("apocalypse").toInt,
                    params("round").toInt
                )
            case "React" =>
                react(
                    params("generation").toInt,
                    View(params("view")),
                    params
                )
            case "Goodbye" =>
                goodbye(
                    params("energy").toInt
                )
            case _ =>
                "" // OK
        }
    }

    def welcome(name: String, path: String, apocalypse: Int, round: Int) = ""

    def react(generation: Int, view: View, params: Map[String, String]) =
        if( generation == 0 ) reactAsMaster(view, params)
        else reactAsSlave(view, params)

    def goodbye(energy: Int) = ""

    def reactAsMaster(view: View, params: Map[String, String]) = "Status(text=Master)"

    def reactAsSlave(view: View, params: Map[String, String]) = "Status(text=Slave)"
}

/** Utility methods for parsing strings containing a single command of the format
  * "Command(key=value,key=value,...)"
  */
object CommandParser {
    /** "Command(..)" => ("Command", Map( ("key" -> "value"), ("key" -> "value"), ..}) */
    def apply(command: String): (String, Map[String, String]) = {
        /** "key=value" => ("key","value") */
        def splitParameterIntoKeyValue(param: String): (String, String) = {
            val segments = param.split('=')
            (segments(0), if(segments.length>=2) segments(1) else "")
        }

        val segments = command.split('(')
        if( segments.length != 2 )
            throw new IllegalStateException("invalid command: " + command)
        val opcode = segments(0)
        val params = segments(1).dropRight(1).split(',')
        val keyValuePairs = params.map(splitParameterIntoKeyValue).toMap
        (opcode, keyValuePairs)
    }
}

case class View(cells: String)
