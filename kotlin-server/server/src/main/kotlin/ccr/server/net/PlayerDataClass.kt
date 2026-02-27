package ccr.server.net

// C++: PlayerDataClass (playerdata.h / playerdata.cpp) — minimal server port
class PlayerDataClass {

    // C++: GameObjReference GameObj
    // @JvmName avoids clash with fun getGameObj() / fun setGameObj() below
    @get:JvmName("gameObjField") @set:JvmName("setGameObjField")
    var gameObj: SmartGameObj? = null

    // C++: void Set_GameObj(SmartGameObj*)
    fun setGameObj(obj: SmartGameObj?) { gameObj = obj }

    // C++: SmartGameObj* Get_GameObj()
    fun getGameObj(): SmartGameObj? = gameObj

    // C++: void Stats_Add_Powerup() — increments the player's powerup pickup counter
    // FIXME: real implementation updates persistent stats when PlayerDataClass is fully ported
    fun statsAddPowerup() {}
}
