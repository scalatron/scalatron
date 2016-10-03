package scalatron.botwar.renderer

class InteractivelyAdjustableSettings {
    var sleepTimeBetweenSteps = 0L      // for debugging, keyboard-activated
    var drawBotHorizon = false          // draw the semi-transparent view of bots?
    var abortThisRound = false          // abort this round (then reload plug-ins and re-start...); won't update roundsPlayed or leaderboard

    var introStartTime = System.currentTimeMillis

    var playerOfInterestOpt: Option[(Int, String)] = None // (rank,name) of bot of interest

    /** Steps the "player of interest" indicator to the next player (called by keyboard handler). */
    def nextPlayerOfInterest(): Unit = {
        playerOfInterestOpt match {
            case None => playerOfInterestOpt = Some((0, ""))
            case Some((rank, name)) => playerOfInterestOpt = Some((rank + 1, ""))
        }
    }

    /** Steps the "player of interest" indicator to the previous player (called by keyboard handler). */
    def prevPlayerOfInterest(): Unit = {
        playerOfInterestOpt match {
            case None => // OK
            case Some((rank, name)) =>
                if( rank <= 0 ) playerOfInterestOpt = None
                else playerOfInterestOpt = Some((rank - 1, ""))
        }
    }
}
