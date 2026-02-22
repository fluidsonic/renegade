package ccr.server

import ccr.server.net.BaseGameObj
import ccr.server.net.BuildingGameObj
import ccr.server.net.SoldierGameObj

/**
 * Port of C++ GameObjManager (gameobjmanager.h/cpp).
 * Owns all registered game objects and drives their Think() loops.
 */
class GameObjManager {

    // C++: SList<BaseGameObj> GameObjList — all game objects
    private val gameObjList = mutableListOf<BaseGameObj>()

    // C++: SList<BuildingGameObj> BuildingGameObjList
    private val buildingList = mutableListOf<BuildingGameObj>()

    // C++: SList<SoldierGameObj> StarGameObjList — human-controlled soldiers
    private val starList = mutableListOf<SoldierGameObj>()

    // C++: GameObjManager::Add — called when registering an object
    fun add(obj: BaseGameObj) { gameObjList.add(0, obj) }  // prepend like C++ SList
    fun remove(obj: BaseGameObj) { gameObjList.remove(obj) }

    fun addBuilding(b: BuildingGameObj) { buildingList.add(b) }
    fun removeBuilding(b: BuildingGameObj) { buildingList.remove(b) }

    fun addStar(s: SoldierGameObj) { starList.add(s) }
    fun removeStar(s: SoldierGameObj) { starList.remove(s) }

    // C++: GameObjManager::Think — iterate all, call Think()
    fun think(deltaSeconds: Float) {
        for (obj in gameObjList.toList()) {
            obj.think(deltaSeconds)
        }
    }

    // C++: GameObjManager::Get_Star_Game_Obj_List (used by Distribute_Funds)
    fun getStarList(): List<SoldierGameObj> = starList

    fun getBuildingList(): List<BuildingGameObj> = buildingList
    fun getAllObjects(): List<BaseGameObj> = gameObjList

    // C++: GameObjManager::Init — called at Pre_Load_Level
    fun destroyAll() {
        gameObjList.clear()
        buildingList.clear()
        starList.clear()
    }
}
