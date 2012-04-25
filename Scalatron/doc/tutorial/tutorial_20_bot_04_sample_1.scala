// Tutorial Bot #4: Expanded Input Parser

class ControlFunction {
    def respond(input: String) = {
        val tokens = input.split('(')
        val opcode = tokens(0)
        if(opcode=="React") {
            val rest = tokens(1).dropRight(1)               // "key=value,key=value,key=value"
            val params = rest.split(',')                    // = Array("key=value", "key=value", ...)
            val strPairs = params.map(s => s.split('='))    // = Array( Array("key","value"), Array("key","value"), ..)
            val kvPairs = strPairs.map(a => (a(0),a(1)))    // = Array( ("key","value"), ("key","value"), ..)
            val paramMap = kvPairs.toMap                    // = Map( "key" -> "value", "key" -> "value", ..)

            val energy = paramMap("energy").toInt
            "Status(text=Energy:" + energy + ")"
        } else {
            ""
        }
    }
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

