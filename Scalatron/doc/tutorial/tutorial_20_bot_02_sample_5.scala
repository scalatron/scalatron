// Tutorial Bot #2: Counting Cycles
// Step 4: correct implementation

class ControlFunction {
    var n = 0
    def respond(input: String) = {
        val output = "Status(text=" + n + ")"   // temp value
        n += 1                                  // now increments after use
        output                                  // yield
    }
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}