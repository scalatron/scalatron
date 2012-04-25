// Tutorial Bot #3: Checking the Opcode

class ControlFunction {
    def respond(input: String) = {
        val tokens = input.split('(')   // split at '(', returns Array[String]
        if(tokens(0)=="React") {        // token(0): 0th element of array
            "Move(direction=1:0)"       // response if true
        } else {
            ""                          // response if false
        }
    }
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

