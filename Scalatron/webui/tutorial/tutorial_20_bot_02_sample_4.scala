// Tutorial Bot #2: Counting Cycles
// Step 3: wrong return type -- will not compile

class ControlFunction {
    var n = 0
    def respond(input: String) : String = {
        "Status(text=" + n + ")"
        n += 1                          // BUG: yields Int (the updated n), not string
    }
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}