package ccr.server.net

// C++: PlayerDataClass (playerdata.h / playerdata.cpp) — minimal server port
// Converted to interface so cPlayer (Player.kt) can extend both NetworkObjectClass and PlayerDataClass.
interface PlayerDataClass {

    // C++: GameObjReference GameObj — the soldier this player data is attached to
    var gameObj: SmartGameObj?

    // C++: void Stats_Add_Powerup() — increments the player's powerup pickup counter
    fun statsAddPowerup() {}

    // C++: PlayerDataClass::Give_Money — adds funds to the player's account and marks the
    // player object dirty so the new balance is sent to all clients.
    // Declared here (not as an extension) so cPlayer (Player.kt) can override it
    // and Distribute_Funds_To_Each_Teammate dispatches polymorphically.
    fun addMoney(amount: Float) {}

    // C++: Stats_Add_* — server-side stat tracking stubs
    fun statsAddBuildingDestroyed() {}
    fun statsAddVehicleDestroyed() {}
    fun statsAddEnemyKilled() {}
    fun statsAddAllyKilled() {}
    fun statsAddKillFromVehicle() {}
    fun statsAddSquish() {}
    fun statsAddHeadShot() {}
    fun statsAddHeadHit() {}
    fun statsAddTorsoShot() {}
    fun statsAddTorsoHit() {}
    fun statsAddArmShot() {}
    fun statsAddArmHit() {}
    fun statsAddLegShot() {}
    fun statsAddLegHit() {}
    fun statsAddCrotchShot() {}
    fun statsAddCrotchHit() {}
    fun statsAddGameTime(seconds: Float) {}
    fun statsAddVehicleTime(seconds: Float) {}
    fun statsSetFinalHealth(health: Float) {}
    fun getPunishTimer(): Float = 0f
    fun incPunishTimer(seconds: Float) {}
}
