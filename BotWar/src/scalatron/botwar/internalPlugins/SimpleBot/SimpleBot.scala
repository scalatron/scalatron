/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar.internalPlugins.SimpleBot


class SimpleBot() {
    /** This method is invoked by the game server to interact with the plug-in.
      * The input will be a string of the format "Opcode(param=value,param=value,...)"
      * The output must be a string that is empty or also "Opcode(param=value,param=value,...)"
      */
    def respond(input: String): String = "Status(text=Simple Bot)"
}


