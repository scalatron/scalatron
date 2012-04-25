// Tutorial Bot #4: Expanded Input Parser

class ControlFunction {
    def respond(input: String) = {
        val tokens = input.split('(')
        val opcode = tokens(0)
        if(opcode=="React") {
            val paramMap =
                tokens(1).dropRight(1).split(',').map(_.split('='))
                .map(a => (a(0), a(1))).toMap
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

