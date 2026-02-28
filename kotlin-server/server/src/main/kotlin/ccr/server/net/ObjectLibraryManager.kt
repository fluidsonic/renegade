package ccr.server.net

import ccr.server.defs.BeaconGameObjDef
import ccr.server.defs.BuildingGameObjDef
import ccr.server.defs.C4GameObjDef
import ccr.server.defs.CinematicGameObjDef
import ccr.physics.defs.DefinitionClass
import ccr.server.defs.PhysicalGameObjDef
import ccr.server.defs.PowerUpGameObjDef
import ccr.server.defs.SimpleGameObjDef
import ccr.server.defs.SoldierGameObjDef
import ccr.server.defs.VehicleGameObjDef

// C++: ObjectLibraryManager (objectlibrary.h)
object ObjectLibraryManager {
    // C++: ObjectLibraryManager::Create_Object(int def_id)
    fun createObject(defId: Int): PhysicalGameObj? {
        val def = DefinitionMgrClass.findDefinition(defId) ?: return null
        if (!isGameObjectDef(def)) return null
        return createFromDefinition(def)
    }

    // C++: ObjectLibraryManager::Create_Object(const char* name)
    fun createObject(name: String): PhysicalGameObj? {
        val def = DefinitionMgrClass.findTypedDefinition(name, 0) ?: return null
        if (!isGameObjectDef(def)) return null
        return createFromDefinition(def)
    }

    private fun isGameObjectDef(def: DefinitionClass): Boolean =
        def is PhysicalGameObjDef || def is BuildingGameObjDef

    private fun createFromDefinition(def: DefinitionClass): PhysicalGameObj? = when (def) {
        is SoldierGameObjDef   -> SoldierGameObj().apply { definition = def }
        is VehicleGameObjDef   -> VehicleGameObj().apply { definition = def }
        is PowerUpGameObjDef   -> PowerUpGameObj().apply { definition = def }
        is C4GameObjDef        -> C4GameObj().apply { definition = def }
        is BeaconGameObjDef    -> BeaconGameObj().apply { definition = def }
        is SimpleGameObjDef    -> SimpleGameObj().apply { definition = def }
        is CinematicGameObjDef -> CinematicGameObj().apply { definition = def }
        else                   -> null
    }
}
