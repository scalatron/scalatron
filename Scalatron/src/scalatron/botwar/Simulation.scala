/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

import scalatron.scalatron.impl.Plugin
import akka.util.Duration
import akka.dispatch.{ExecutionContext, Future, Await}


/** Traits for generic simulations, of which a game like BotWar is an example.
  */
object Simulation
{
    /** Simulation.UntypedState: non-polymorphic base trait for State that simplifies passing State to contexts
      * where we don't want to introduce the types of S and R.
      */
    trait UntypedState

    /** Simulation.State: base traits for simulation state implementations.
      * @tparam S type of the simulation state implementation (extends Simulation.State)
      * @tparam R type of the result returned by the simulator (arbitrary)
      */
    trait State[S <: State[S, R], R] extends UntypedState {
        def step(executionContextForUntrustedCode: ExecutionContext) : Either[S, R]
    }


    /** Simulation.Factory: base traits for simulation state factory implementations.
      * @tparam S type of the simulation state implementation (extends Simulation.State)
      * @tparam R type of the result returned by the simulator (arbitrary)
      */
    trait Factory[S <: State[S, R], R] {
        def createInitialState(randomSeed: Int, plugins: Iterable[Plugin.External])(executionContextForUntrustedCode: ExecutionContext) : S
    }

    /** Simulation.Runner: a generic runner for simulations that uses .step() to iteratively
      * compute new simulation states.
      * @tparam S type of the simulation state implementation (extends Simulation.State)
      * @tparam R type of the result returned by the simulator (arbitrary)
      */
    case class Runner[S <: State[S, R], R](
        factory: Factory[S, R],
        stepCallback: S => Boolean, // callback invoked at end of every simulation step; if it returns false, the sim terminates without result
        resultCallback: (S, R) => Unit ) //  callback invoked after the end of very last simulation step
    {
        /** @param plugins the collection of external plug-ins to bring into the simulation
          * @param randomSeed the random seed to use for initializing the simulation
          * @param executionContextForTrustedCode execution context whose threads are not sandboxed (e.g. actor system)
          * @param executionContextForUntrustedCode execution context whose threads are sandboxed by the security manager
          * @return an optional simulation result (if the simulation was not prematurely aborted)
          */
        def apply(
            plugins: Iterable[Plugin.External],
            randomSeed: Int
        )(
            executionContextForTrustedCode: ExecutionContext,
            executionContextForUntrustedCode: ExecutionContext
        ): Option[R] =
        {
            var currentState = factory.createInitialState(randomSeed, plugins)(executionContextForUntrustedCode) // state at entry of loop turn
            var priorStateOpt : Option[S] = None // result of state.step() at exit of prior loop turn
            var finalResult: Option[R] = None

            var running = true
            while( running ) {
                // we'll use Akka Futures to compute the next state concurrently with the callback on the prior state.
                // the callback will generally render the prior state to the screen.

                // process state update, returns either next state or result
                val stepFuture = Future( { currentState.step(executionContextForUntrustedCode) } )(executionContextForTrustedCode) // compute next state

                // process callback (usually rendering) on prior state, returns true if to continue simulating, false if not
                val callbackFuture = Future( {
                    priorStateOpt match {
                        case None => true // there is no state to call back about (e.g. nothing to render)
                        case Some(priorState) => stepCallback(priorState)
                    }
                } )(executionContextForTrustedCode)

                // let the processing complete
                val stepResult = Await.result(stepFuture, Duration.Inf)
                val callbackAllowsContinuation = Await.result(callbackFuture, Duration.Inf)

                // work with the results
                val simulationContinues = stepResult match {
                    case Left(updatedState) =>
                        priorStateOpt = Some(currentState)
                        currentState = updatedState
                        true

                    case Right(gameResult) =>
                        resultCallback(currentState, gameResult)
                        finalResult = Some(gameResult)
                        false
                }

                running = callbackAllowsContinuation && simulationContinues
            }
            finalResult
        }
    }


}