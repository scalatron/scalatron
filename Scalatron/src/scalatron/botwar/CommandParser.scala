package scalatron.botwar

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
/** Utility methods for parsing strings containing a single command of the format
  *  "Command(key=value,key=value,...)"
  */
object CommandParser {
    /** "Command(..)" => ("Command", Map( ("key" -> "value"), ("key" -> "value"), ..}) */
    def splitCommandIntoOpcodeAndParameters(command: String): (String, Map[String, String]) = {
        val segments = command.split('(')
        if( segments.length != 2 )
            throw new IllegalStateException("invalid command: \"" + command + "\"")
        val opcode = segments(0)
        val params = segments(1).dropRight(1).split(',')
        val keyValuePairs = params.map(splitParameterIntoKeyValue).toMap
        (opcode, keyValuePairs)
    }

    /** "key=value" => ("key","value") */
    def splitParameterIntoKeyValue(param: String): (String, String) = {
        val segments = param.split('=')
        (segments(0), if( segments.length >= 2 ) segments(1) else "")
    }
}
