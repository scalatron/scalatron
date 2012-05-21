/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.botwar

object Constants
{
    val GameName = "BotWar"

    val MasterHorizonHalfSize = 15
    val SlaveHorizonHalfSize = 10


    object StunTime {
        val MasterHitsWall  = 4 // cycles
        val SlaveHitsWall   = 4 // cycles
    }

    val MinBlastRadius = 2
    val MaxBlastRadius = 10

    object Energy {
        val Initial = 1000

        val PainForPlayerFromHittingWall  = -10

        val ValueForMasterFromEatingEnemySlave = 150
        val ValueForPlayerFromEatingGoodPlant = 100
        val ValueForPlayerFromEatingGoodBeast = 200

        val PainForBoth_Player_vs_BadBeast  = -150
        val PainForPlayerFromSteppingOnBadPlant  = -100

        val SlaveDepletionCycleSpacing = 4      // slave energy is reduced every 4th cycle...
        val SlaveDepletionPerSpacedCycle = 1    // ...by this much

        val ExplosionDamageFactor = 200        // boost factor applied to energy per area
    }
}
