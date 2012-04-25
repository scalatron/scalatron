/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar


/** Utility methods for parsing strings containing a sequence of multiple commands of the format:
 *  "Command(key=value,key=value,...)|Command(key=value,key=value,...)|..."
 */
object MultiCommandParser
{
    // map from opcode to parameters (key/value pairs)
    type MultiCommand = Map[String,Map[String,String]]

    /** "Command(..)|Command(..)|.." => Map[opcode,Map[paramKey,paramValue]] */
    def splitStringIntoParsedCommandMap(controlFunctionResponse: String) : MultiCommand =
        if(controlFunctionResponse.isEmpty) {
            Map.empty
        } else {
            val commands = splitStringIntoCommandStrings(controlFunctionResponse)
            val opcodesAndParameters = commands.map( CommandParser.splitCommandIntoOpcodeAndParameters )
            opcodesAndParameters.toMap
        }

    /** "Command(..)|Command(..)|.." => {"Command(..)", "Command(..)", ..} */
    def splitStringIntoCommandStrings(action: String) : Array[String] = {
        val commands = action.split('|')
        if( commands.length < 1 ) throw new IllegalStateException("string is not a command sequence: " + action)
        commands
    }
}
