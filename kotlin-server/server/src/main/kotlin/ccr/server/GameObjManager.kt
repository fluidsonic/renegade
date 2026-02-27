package ccr.server

import ccr.server.defs.BuildingGameObjDef
import ccr.server.level.DefinitionRegistry
import ccr.server.net.BaseGameObj
import ccr.server.net.BuildingGameObj
import ccr.server.net.DamageableGameObj
import ccr.server.net.PhysicalGameObj
import ccr.server.net.ScriptableGameObj
import ccr.server.net.SmartGameObj
import ccr.server.net.SoldierGameObj

/**
 * Port of C++ GameObjManager (gameobjmanager.h/cpp).
 * Owns all registered game objects and drives their Think() loops.
 *
 * Implemented as a Kotlin object (singleton) because the new NetworkObject subclass hierarchy
 * calls GameObjManager.add(this) / GameObjManager.remove(this) in BaseGameObj.init{} and
 * destruct() — these are class-level init blocks that fire before any external reference to
 * the instance is available, so the manager must be globally accessible without an instance.
 */
object GameObjManager {

    // C++: SList<BaseGameObj> GameObjList — all game objects
    private val gameObjList = mutableListOf<BaseGameObj>()

    // C++: SList<BuildingGameObj> BuildingGameObjList
    private val buildingList = mutableListOf<BuildingGameObj>()

    // C++: SList<SoldierGameObj> StarGameObjList — human-controlled soldiers
    private val starList = mutableListOf<SoldierGameObj>()

    // C++: Is_Cinematic_Freeze_Active
    var isCinematicFreezeActive: Boolean = false

    // C++: GameObjManager::Add — called when registering an object
    fun add(obj: BaseGameObj) { gameObjList.add(0, obj) }  // prepend like C++ SList
    fun remove(obj: BaseGameObj) { gameObjList.remove(obj) }

    fun addBuilding(b: BuildingGameObj) { buildingList.add(b) }
    fun removeBuilding(b: BuildingGameObj) { buildingList.remove(b) }

    fun addStar(s: SoldierGameObj) { starList.add(s) }
    fun removeStar(s: SoldierGameObj) { starList.remove(s) }

    // C++: GameObjManager::Think — iterate all, call Think() (gameobjmanager.cpp:212-248)
    // Note: BaseGameObj.think() takes no arguments in the new hierarchy.
    fun think(deltaSeconds: Float) {
        for (obj in gameObjList.toList()) {
            if (isCinematicFreezeActive && obj.enableCinematicFreeze) continue
            if (obj.isHibernating()) continue
            obj.think()
        }
    }

    // C++: GameObjManager::Post_Think — iterate all, call Post_Think() (gameobjmanager.cpp:255-278)
    fun postThink() {
        for (obj in gameObjList.toList()) {
            if (isCinematicFreezeActive && obj.enableCinematicFreeze) continue
            if (!obj.isHibernating() && obj.isPostThinkAllowed) {
                obj.postThink()
            }
        }
    }

    // C++: GameObjManager::Get_Star_Game_Obj_List (used by Distribute_Funds)
    fun getStarList(): List<SoldierGameObj> = starList

    fun getBuildingList(): List<BuildingGameObj> = buildingList
    fun getBuildingGameObjList(): List<BuildingGameObj> = buildingList
    fun getAllObjects(): List<BaseGameObj> = gameObjList
    fun getGameObjs(): List<BaseGameObj> = gameObjList

    // C++: GameObjManager::Get_Smart_Game_Obj_List — iterate SmartGameObj instances
    fun getSmartGameObjs(): List<SmartGameObj> = gameObjList.filterIsInstance<SmartGameObj>()
    fun getSmartGameObjList(): List<SmartGameObj> = getSmartGameObjs()

    // C++: GameObjManager::Find_Scriptable_Game_Obj — find ScriptableGameObj by network ID
    fun findScriptableGameObj(networkId: Int): ScriptableGameObj? {
        for (obj in gameObjList) {
            if (obj is ScriptableGameObj && obj.networkId == networkId) return obj
        }
        return null
    }

    // C++: GameObjManager::Find_Object — find DamageableGameObj by network ID
    fun findObject(networkId: Int): DamageableGameObj? {
        for (obj in gameObjList) {
            if (obj is DamageableGameObj && obj.networkId == networkId) return obj
        }
        return null
    }

    // C++: GameObjManager::Find_Smart_Game_Obj — find SmartGameObj by network ID
    fun findSmartGameObj(networkId: Int): SmartGameObj? {
        for (obj in gameObjList) {
            if (obj is SmartGameObj && obj.networkId == networkId) return obj
        }
        return null
    }

    // C++: GameObjManager::Find_Physical_Game_Obj — find PhysicalGameObj by network ID
    fun findPhysicalGameObj(networkId: Int): PhysicalGameObj? {
        for (obj in gameObjList) {
            if (obj is PhysicalGameObj && obj.networkId == networkId) return obj
        }
        return null
    }

    // C++: GameObjManager::Update_Building_Collection_Spheres (gameobjmanager.cpp:513-563)
    // O(n²) pass: for each building, shrinks radius to min(50, distance to nearest same-prefix building).
    fun updateBuildingCollectionSpheres(definitions: DefinitionRegistry) {
        for (building in buildingList) {
            val def = definitions.findById(building.definition?.id ?: 0u) as? BuildingGameObjDef ?: continue
            val prefix = def.meshPrefix
            if (prefix.isEmpty()) continue
            var maxRadius = 50f
            for (other in buildingList) {
                if (other === building) continue
                val otherDef = definitions.findById(other.definition?.id ?: 0u) as? BuildingGameObjDef ?: continue
                if (!otherDef.meshPrefix.equals(prefix, ignoreCase = true)) continue
                val dx = building.position.x - other.position.x
                val dy = building.position.y - other.position.y
                val dz = building.position.z - other.position.z
                val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                maxRadius = minOf(maxRadius, dist)
            }
            building.sphereRadius = maxRadius
        }
    }

    // C++: GameObjManager::Init — called at Pre_Load_Level
    fun destroyAll() {
        gameObjList.clear()
        buildingList.clear()
        starList.clear()
    }
}
