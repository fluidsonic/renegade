package ccr.server.net

// C++: PlayerDataClass (playerdata.h / playerdata.cpp) — minimal server port
// Converted to interface so cPlayer (Player.kt) can extend both NetworkObjectClass and PlayerDataClass.
interface PlayerDataClass {

    // C++: GameObjReference GameObj — the soldier this player data is attached to
    var gameObj: SmartGameObj?

    // C++: void Stats_Add_Powerup() — increments the player's powerup pickup counter
    fun statsAddPowerup() {}
}
