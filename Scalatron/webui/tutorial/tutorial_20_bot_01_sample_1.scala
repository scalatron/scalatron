// Tutorial Bot #1: Hello World

class ControlFunction {
    def respond(input: String) = "Status(text=Hello World)"
}

class ControlFunctionFactory {
    def create = new ControlFunction().respond _
}

