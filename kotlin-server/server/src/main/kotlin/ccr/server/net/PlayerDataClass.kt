package ccr.server.net

// C++: PlayerDataClass (playerdata.h / playerdata.cpp) — minimal server port
class PlayerDataClass {

    // C++: GameObjReference GameObj
    var gameObj: SmartGameObj? = null

    // C++: void Set_GameObj(SmartGameObj*)
    fun setGameObj(obj: SmartGameObj?) { gameObj = obj }

    // C++: SmartGameObj* Get_GameObj()
    fun getGameObj(): SmartGameObj? = gameObj
}
