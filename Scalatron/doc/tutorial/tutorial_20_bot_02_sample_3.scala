// Tutorial Bot #2: Counting Cycles
// Step 2: field incrementation before String concatenation

class ControlFunction {
    var n = 0
    def respond(input: String) = {  // curly braces for scope
        n += 1                      // increment counter
        "Status(text=" + n + ")"
    }
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}