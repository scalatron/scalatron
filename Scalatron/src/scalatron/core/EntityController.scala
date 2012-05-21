package scalatron.core

/** An entity controller is a wrapper provided by Scalatron for a (bot) control function exposed by a (bot) plug-in.
  */
trait EntityController
{
    /** The name of the entity controller, which is also the name of the plug-in from which it was loaded and the
      * name of the player with which it is associated.
      * @return the name of the plug-in, which is also the name of the associated player, e.g. "Daniel". */
    def name: String

    /** @return the control function factory implemented by the plug-in. */
    def respond(input: String) : String

    /** Returns a copy of this instance that uses the given replacement control function. Use this facility for
      * example to replace the control function of entity controllers that caused security violations.
      * @param controlFunction the replacement control function
      * @return a copy of this instance that uses the given replacement control function
      */
    def withReplacedControlFunction(controlFunction: String => String) : EntityController
}
