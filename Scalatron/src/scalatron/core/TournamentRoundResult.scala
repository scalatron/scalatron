package scalatron.core

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
/** Map from user name to score. */
case class TournamentRoundResult(map: Map[String, Int])


object TournamentRoundResult
{
    /** Merges two game result instances into a new, aggregated instance. */
    def merge(a: AggregateResult, b: TournamentRoundResult) = {
        /** Merges two result maps by adding up the scores for all matching player names. */
        def mergeMaps(mapA: Map[String, (Int, Int)], mapB: Map[String, Int]) =
            (mapA /: mapB)((sum, add) => {
                val name = add._1
                val additionalScore = add._2
                val existingTotalScoreAndCount = sum.get(name).getOrElse((0, 0))
                val sumTotalScore = existingTotalScoreAndCount._1 + additionalScore
                val sumTotalCount = existingTotalScoreAndCount._2 + 1
                sum.updated(name, (sumTotalScore, sumTotalCount))
            })

        AggregateResult(mergeMaps(a.map, b.map), a.totalRounds + 1)
    }


    /** Map: from name to (totalScore,roundsParticipated). */
    case class AggregateResult(map: Map[String, (Int, Int)], totalRounds: Int)
    {
        /** Returns a map containing the name and average score of each player. */
        def average: Map[String, Int] = map.map(entry => (entry._1, entry._2._1 / entry._2._2))

        /** Returns a map containing the name and average score of each player that actually
          * participated in all rounds. */
        def averageMustHavePlayedAllRounds: Map[String, Int] =
            map
            .filter(entry => entry._2._2 == totalRounds)
            .map(entry => (entry._1, entry._2._1 / entry._2._2))
    }


    object AggregateResult
    {
        val Zero = AggregateResult(Map.empty[String, (Int, Int)], 0)

        /** Merges two game result instances into a new, aggregated instance. */
        def merge(a: AggregateResult, b: AggregateResult) = {
            /** Merges two result maps by adding up the scores for all matching player names. */
            def mergeMaps(mapA: Map[String, (Int, Int)], mapB: Map[String, (Int, Int)]) =
                (mapA /: mapB)((sum, add) => {
                    val name = add._1
                    val additionalScoreAndCount = add._2
                    val existingTotalScoreAndCount = sum.get(name).getOrElse((0, 0))
                    val sumTotalScore = existingTotalScoreAndCount._1 + additionalScoreAndCount._1
                    val sumTotalCount = existingTotalScoreAndCount._2 + additionalScoreAndCount._2
                    sum.updated(name, (sumTotalScore, sumTotalCount))
                })

            AggregateResult(mergeMaps(a.map, b.map), a.totalRounds + b.totalRounds)
        }
    }


}
