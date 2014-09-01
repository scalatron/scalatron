// Tutorial Bot 02 - Counting Cycles

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

class ControlFunction {
    var n = 0
    def respond(input: String) = {
        val output = "Status(text=" + n + ")"   // temp value
        n += 1                                  // now increments after use
        output                                  // yield
    }
}
