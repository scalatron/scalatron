package scalatron.botwar

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
object Protocol {
    object ServerOpcode {
        val Welcome = "Welcome"
        val React = "React"
        val Goodbye = "Goodbye"

        object ParameterName {
            val Master = "master"
        }
    }

    object PluginOpcode {
        val Nop = "Nop"             // used internally by server to handle empty control function output
        val Move = "Move"
        val Spawn = "Spawn"
        val Say = "Say"
        val Status = "Status"
        val Set = "Set"
        val Explode = "Explode"
        val Log = "Log"
        val Disable = "Disable"
        val MarkCell = "MarkCell"
        val DrawLine = "DrawLine"

        object ParameterName {
            val Direction = "direction"
            val Text = "text"
            val BlastRadius = "size"
            val Position = "position"
            val From = "from"
            val To = "to"
            val Color = "color"
        }
    }

    object PropertyName {
        val Status = "status"
        val Energy = "energy"
        val Time = "time"
        val View = "view"
        val Debug = "debug"
        val Bonked = "bonked"
        val Generation = "generation"
        val Name = "name"

        val ListOfReserved =
            List(
                Energy, Time, View, Generation, Name,
                PluginOpcode.ParameterName.Direction,
                ServerOpcode.ParameterName.Master
            )

        def isReserved(name: String) = Protocol.PropertyName.ListOfReserved.contains(name)

    }
}
