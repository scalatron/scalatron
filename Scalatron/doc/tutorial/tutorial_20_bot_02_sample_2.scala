// Tutorial Bot #2: Counting Cycles
// Step 1: mutable field, but no incrementation

class ControlFunction {
    var n = 0           // declare mutable field
    def respond(input: String) = "Status(text=" + n + ")"
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}