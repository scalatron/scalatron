package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import scalatron.core.Scalatron.LeaderBoard
import scalatron.core.TournamentRoundResult.AggregateResult
import scalatron.core.{Simulation, TournamentRoundResult}


object TournamentState
{
    val Empty = new TournamentState
}


class TournamentState
{
    var roundsPlayed = 0
    var results = List.empty[TournamentRoundResult]

    /** we maintain a reference to the most recently computed tournament state.
      * This can be streamed to a display client to render the tournament remotely.
      */
    private var mostRecentStateOpt: Option[Simulation.UntypedState] = None
    def updateMostRecentState(mostRecentState: Simulation.UntypedState) { mostRecentStateOpt = Some(mostRecentState) }
    def getMostRecentStateOpt = mostRecentStateOpt


    /** we cache a leaderboard holding the winners across the most recent 1,5,20 and all rounds. */
    var leaderBoard: LeaderBoard = computeLeaderboard


    /** Adds the most recent result to the tournament result list, at the head. */
    def addResult(result: TournamentRoundResult) {
        roundsPlayed += 1
        results = result :: results
        assert(results.length == roundsPlayed)
        leaderBoard = computeLeaderboard
    }

    /** Returns tournament round results for up to the given number of rounds, starting with
      * the most recent rounds. */
    def roundResults(maxRounds: Int): Iterable[TournamentRoundResult] = results.take(maxRounds)


    /** Returns an aggregate of tournament round results for up to the given number of rounds,
      * starting with the most recent rounds. The aggregate contains the cumulative score for
      * each player. */
    def aggregateResult(maxRounds: Int): AggregateResult = {
        val mostRecentResults = roundResults(maxRounds)
        mostRecentResults.aggregate(AggregateResult.Zero)(TournamentRoundResult.merge, AggregateResult.merge)
    }


    /** Computes an array with four slots, holding the (name,score) of the most recent 1, 5,
      * 20 and all rounds.
      */
    private def computeLeaderboard: LeaderBoard = {
        def winnersOverLastNCycles(n: Int): Array[(String, Int)] = {
            val aggregatedResult = aggregateResult(n)
            aggregatedResult.averageMustHavePlayedAllRounds.toArray.sortBy(-_._2)
        }

        Array(
            (1, winnersOverLastNCycles(1)),
            (5, winnersOverLastNCycles(5)),
            (20, winnersOverLastNCycles(20)),
            (Int.MaxValue, winnersOverLastNCycles(Int.MaxValue))
        )
    }

}

