package scalatron.scalatron.impl

import scalatron.core.EntityController
import java.util.concurrent.TimeoutException
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._

case class EntityControllerImpl(name: String, controlFunction: String => String) extends EntityController {
    def respond(input: String) = controlFunction(input)
    def withReplacedControlFunction(controlFunction: (String) => String) = copy(controlFunction = controlFunction)
}

object EntityControllerImpl
{
    def fromPlugins(plugins: Iterable[Plugin])(implicit executionContextForUntrustedCode: ExecutionContext) : Iterable[EntityController] = {
        // generate players' control functions -- use Akka to work this out
        // isolate the control function factory invocation via the untrusted thread pool
        val future = Future.traverse(plugins)(plugin => Future {
            try {
                val controlFunction = plugin.controlFunctionFactory.apply()
                Some((plugin, controlFunction))
            } catch {
                case t: Throwable =>
                    System.err.println("error: exception while instantiating control function of plugin '" + plugin.name + "': " + t)
                    None
            }
        })

        // Note: an overall timeout across all bots is a temporary solution - we want timeouts PER BOT
        val pluginsAndControlFunctions =
            try {
                val result = Await.result(future, 2000 millis)      // generous timeout - note that this is over ALL plug-ins
                result.flatten     // remove failed instantiations
            } catch {
                case t: TimeoutException =>
                    System.err.println("warning: timeout while instantiating control function of one of the plugins")
                    Iterable.empty          // temporary - disables ALL bots, which is not the intention
            }

        pluginsAndControlFunctions.map(pair => EntityControllerImpl(pair._1.name, pair._2) : EntityController)

    }
}
