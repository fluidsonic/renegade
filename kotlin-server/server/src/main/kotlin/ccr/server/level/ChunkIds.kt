package ccr.server.level

/**
 * Chunk ID constants for Renegade map files.
 *
 * Sources:
 *  - savegame.cpp / combatsaveload.cpp — LDD chunk IDs
 *  - persistfactory.h — SIMPLEFACTORY chunk IDs
 *  - pscene_saveload.cpp / physstaticsavesystem.h — LSD physics chunk IDs
 *  - wwphysids.h / saveloadids.h — subsystem IDs
 *  - spawn.cpp — spawner chunk IDs
 *  - combatchunkid.h — game object persist factory IDs
 *  - Pathfind.cpp — pathfinding chunk IDs
 */
object ChunkIds {

    // ─── LDD top-level (combatsaveload.cpp) ────────────────────────────────────
    const val CHUNKID_LEVEL_INFO        = 1011991648u    // 0x3C51C460
    const val CHUNKID_LEVEL_DATA        = 1011991649u    // 0x3C51C461
    const val CHUNKID_GAMEOBJMANAGER    = 916991654u     // 0x36A82EA6 — written by CombatSaveLoadClass
    const val CHUNKID_GAMEOBJ_OBJECTS   = 916991653u     // 0x36A82EA5 — object list inside GAMEOBJMANAGER
    const val CHUNKID_SPAWNERS          = 916991657u     // 0x36A82EA9
    const val CHUNKID_SCRIPTS           = 916991659u     // 0x36A82EAB
    const val CHUNKID_COMBAT_BEGIN      = 0x00040000u

    // ─── SpawnerClass (spawn.cpp) ───────────────────────────────────────────────
    // SpawnManager::Save writes SPAWNER_CHUNKID_DATA wrappers (one per spawner),
    // then SpawnerClass::Save writes SPAWNER_CHUNKID_PARENT + SPAWNER_CHUNKID_VARIABLES inside.
    const val SPAWNER_CHUNKID_DATA      = 1014991133u    // wrapper written by SpawnManager per-spawner
    const val SPAWNER_CHUNKID_VARIABLES_MGR = 1014991134u // SpawnManager's own timer (skip)
    const val SPAWNER_CHUNKID_PARENT    = 1014991053u    // 0x3C56AAED — written by SpawnerClass
    const val SPAWNER_CHUNKID_VARIABLES = 1014991054u    // 0x3C56AAEE — written by SpawnerClass

    // ─── PersistFactory (persistfactory.h) ─────────────────────────────────────
    const val SIMPLEFACTORY_CHUNKID_OBJPOINTER = 0x00100100u
    const val SIMPLEFACTORY_CHUNKID_OBJDATA    = 0x00100101u

    // ─── LSD physics subsystems (saveloadids.h + wwphysids.h) ──────────────────
    const val PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM    = 0x00020000u
    const val PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM = 0x00020001u

    // ─── LDD physics dynamic data subsystem (physdynamicsavesystem.cpp, wwphysids.h) ─
    // PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM = CHUNKID_WWPHYS_BEGIN + 0x50 = 0x00020050
    const val PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM = 0x00020050u
    // PDSSC_CHUNKID_SCENE (physdynamicsavesystem.h) = 0x00007001
    const val PDSSC_CHUNKID_SCENE                    = 0x00007001u
    // PSCENE_DD_CHUNK_DYNAMIC_OBJECTS / _OBJECT (pscene_saveload.cpp) = 0x00890100 / 0x00890101
    const val PSCENE_DD_CHUNK_DYNAMIC_OBJECTS        = 0x00890100u
    const val PSCENE_DD_CHUNK_DYNAMIC_OBJECT         = 0x00890101u
    // PHYSICS_CHUNKID_DECORATIONPHYS = CHUNKID_WWPHYS_BEGIN + 0x100 = 0x00020100
    const val PHYSICS_CHUNKID_DECORATIONPHYS         = 0x00020100u
    // DECOPHYS_CHUNK_DYNAMICPHYS (decophys.cpp) = 0x005060001
    const val DECOPHYS_CHUNK_DYNAMICPHYS             = 0x05060001u
    // DYNAMICPHYS_CHUNK_PHYS (dynamicphys.cpp) = 813001100 = 0x3075698C
    const val DYNAMICPHYS_CHUNK_PHYS                 = 0x3075698Cu
    // PhysicalGameObj CHUNKID_VARIABLES (physicalgameobj.cpp) = 910991145 + 1 = 910991146 = 0x364C9F2A
    const val PHYSICALGAMEOBJ_CHUNKID_VARIABLES      = 910991146u

    // ─── WWAudio subsystem (SoundChunkIDs.h, saveloadids.h) ─────────────────────
    const val WWAUDIO_CHUNKID                          = 0x00030000u  // Range start
    const val WWAUDIO_SOUND3D                          = 0x00030003u  // CHUNKID_SOUND3D
    const val WWAUDIO_STATIC_SAVELOAD                  = 0x00030005u  // Subsystem chunk in LSD

    // ─── Physics scene save/load (pscene_saveload.cpp / physstaticsavesystem.h) ─
    const val PSDSSC_CHUNKID_SCENE    = 0x04433220u
    const val PSDSSC_CHUNKID_PATHFIND = 0x04433221u

    // ─── Physics scene static data chunks (pscene_saveload.cpp, inside PSDSSC_CHUNKID_SCENE) ─
    const val PSCENE_SD_CHUNK_SUNLIGHT  = 0x00004800u
    const val PSCENE_SD_CHUNK_VARIABLES = 0x00004820u
    const val PSCENE_SD_VARIABLE_AMBIENT = 0x00   // micro-chunk ID (uint8)

    // ─── Physics static objects scene (pscene_saveload.cpp, inside PSOSSC_CHUNKID_SCENE) ─
    const val PSOSSC_CHUNKID_SCENE                   = 0x06090609u
    const val PSCENE_SO_CHUNK_STATIC_OBJECTS          = 0x00770100u
    const val PSCENE_SO_CHUNK_STATIC_OBJECT           = 0x00770101u
    const val PSCENE_SO_CHUNK_STATIC_OBJECT_AABLINK   = 0x00770102u
    const val PSCENE_SO_CHUNK_STATIC_LIGHTS           = 0x00770200u
    const val PSCENE_SO_CHUNK_STATIC_LIGHT            = 0x00770201u
    const val PSCENE_SO_CHUNK_STATIC_LIGHT_AABLINK    = 0x00770202u

    // ─── Pathfinding (Pathfind.cpp enum starting at 0x01060635) ─────────────────
    const val PATHFIND_CHUNK_DATABASE               = 0x01060635u
    // 0x01060636 = XXX_CHUNKID_SECTOR (deprecated)
    const val PATHFIND_CHUNK_PORTAL                 = 0x01060637u
    const val PATHFIND_CHUNK_SECTOR_CULLING_SYSTEM  = 0x01060638u
    const val PATHFIND_CHUNK_SECTOR                 = 0x01060639u
    // 0x0106063A = CHUNKID_SECTOR_CULL_TREE (AABTreeCullSystem binary, skipped)
    // 0x0106063B = CHUNKID_SECTOR_LINKAGE (AABTreeCullSystem linkage, skipped)
    const val PATHFIND_CHUNK_SECTOR_OBJECT          = 0x0106063Cu
    const val PATHFIND_CHUNK_HEIGHTDB               = 0x0106063Du
    const val PATHFIND_CHUNK_ACTION_PORTAL          = 0x0106063Eu
    const val PATHFIND_CHUNK_WAYPATH_PORTAL         = 0x0106063Fu
    const val PATHFIND_CHUNK_PATHFIND_SECTOR_OBJECT = 0x01060640u

    // ─── StaticPhysClass save/load (staticphyssaveload.cpp, wwphysids.h) ───────────
    const val STATICPHYS_CHUNK_PHYS              = 0x00DC2F94u
    const val STATICPHYS_CHUNK_VARIABLES         = 0x00DC2F95u

    // ─── PhysClass save/load (physclass.cpp, wwphysids.h) ───────────────────────
    const val PHYS_CHUNK_VARIABLES               = 0x00660055u
    const val PHYS_CHUNK_MODEL                   = 0x00660056u

    // ─── WW3D render object persist (renderobj.cpp) ──────────────────────────────
    const val WW3D_PERSIST_CHUNKID_RENDEROBJ     = 0x00010000u
    const val RENDOBJFACTORY_CHUNKID_VARIABLES   = 0x00555040u

    // ─── Physics persist chunk IDs (wwphysids.h, CHUNKID_WWPHYS_BEGIN=0x00020000) ─
    // Sequential from CHUNKID_WWPHYS_BEGIN + 0x100:
    //   0x100=DECORATIONPHYS, 0x101=HUMANPHYS, 0x102=LIGHTPHYS, 0x103=MOTORCYCLE,
    //   0x104=MOTORVEHICLE, 0x105=PHYS3, 0x106=PROJECTILE, 0x107=RENDEROBJPHYS,
    //   0x108=RIGIDBODY, 0x109=STATICPHYS, 0x10A=WHEELEDVEHICLE, 0x10B=STATICANIMPHYS,
    //   0x10C=TIMEDDECORATIONPHYS, 0x10D=VEHICLEPHYS, 0x10E=TRACKEDVEHICLE,
    //   0x10F=VTOLVEHICLE, 0x110=WAYPATH, 0x111=WAYPOINT, 0x112=DYNAMICANIMPHYS,
    //   0x113=SHAKEABLESTATICPHYS, 0x114=ACCESSIBLEPHYS
    const val PHYSICS_CHUNKID_STATICPHYS            = 0x00020109u
    const val PHYSICS_CHUNKID_STATICANIMPHYS        = 0x0002010Bu  // StaticAnimPhysClass
    const val PHYSICS_CHUNKID_SHAKEABLESTATICPHYS   = 0x00020113u
    const val PHYSICS_CHUNKID_ACCESSIBLEPHYS        = 0x00020114u  // AccessiblePhysClass
    const val PHYSICS_CHUNKID_WAYPATH      = 0x00020110u
    const val PHYSICS_CHUNKID_WAYPOINT     = 0x00020111u
    const val PHYSICS_CHUNKID_DOORPHYS              = 0x00020A00u
    const val PHYSICS_CHUNKID_ELEVATORPHYS          = 0x00020A01u
    const val PHYSICS_CHUNKID_DAMAGEABLESTATICPHYS  = 0x00020A02u
    const val PHYSICS_CHUNKID_BUILDINGAGGREGATE     = 0x00020A03u

    // ─── Game object persist factory IDs (combatchunkid.h) ─────────────────────
    const val GAMEOBJ_C4               = 0x00040102u
    const val GAMEOBJ_SIMPLE           = 0x0004010Au
    const val GAMEOBJ_POWERUP          = 0x00040106u
    const val GAMEOBJ_SOLDIER          = 0x0004010Eu
    const val GAMEOBJ_VEHICLE          = 0x00040110u
    const val GAMEOBJ_SCRIPTZONE       = 0x00040122u
    const val GAMEOBJ_TRANSITION       = 0x00040124u
    const val GAMEOBJ_CINEMATIC        = 0x0004012Bu
    const val GAMEOBJ_DAMAGEZONE       = 0x0004012Du
    const val GAMEOBJ_SPECIALEFFECTS   = 0x0004012Fu
    const val GAMEOBJ_BUILDING         = 0x00040133u
    const val GAMEOBJ_BEACON           = 0x00040135u
    const val GAMEOBJ_BUILDING_REFINERY        = 0x00040137u
    const val GAMEOBJ_BUILDING_POWERPLANT      = 0x00040139u
    const val GAMEOBJ_BUILDING_SOLDIERFACTORY  = 0x0004013Bu
    const val GAMEOBJ_BUILDING_VEHICLEFACTORY  = 0x0004013Du
    const val GAMEOBJ_BUILDING_AIRSTRIP        = 0x0004013Fu
    const val GAMEOBJ_BUILDING_WARFACTORY      = 0x00040141u
    const val GAMEOBJ_BUILDING_COMCENTER       = 0x00040143u
    const val GAMEOBJ_BUILDING_REPAIRBAY       = 0x00040145u
    const val GAMEOBJ_SAMSITE                  = 0x00040108u
    const val GAMEOBJ_SAKURA_BOSS              = 0x00040131u
    const val GAMEOBJ_MENDOZA_BOSS             = 0x00040149u
    const val GAMEOBJ_RAVESHAW_BOSS            = 0x0004014Bu

    // ─── GameObjManager variables (gameobjmanager.cpp) ─────────────────────────
    // CHUNKID_VARIABLES is a sub-chunk of CHUNKID_GAMEOBJMANAGER with the same numeric value.
    const val CHUNKID_GAMEOBJ_VARIABLES        = 916991654u   // same value as CHUNKID_GAMEOBJMANAGER

    // ─── BackgroundMgr / WeatherMgr (combatchunkid.h) ──────────────────────────
    const val CHUNKID_BACKGROUND_MGR = 0x00040126u
    const val CHUNKID_WEATHER_MGR    = 0x00040800u

    /** Returns true if this factory chunk ID represents any kind of building. */
    fun isBuilding(id: UInt) = id in setOf(
        GAMEOBJ_BUILDING, GAMEOBJ_BUILDING_REFINERY, GAMEOBJ_BUILDING_POWERPLANT,
        GAMEOBJ_BUILDING_SOLDIERFACTORY, GAMEOBJ_BUILDING_VEHICLEFACTORY,
        GAMEOBJ_BUILDING_AIRSTRIP, GAMEOBJ_BUILDING_WARFACTORY,
        GAMEOBJ_BUILDING_COMCENTER, GAMEOBJ_BUILDING_REPAIRBAY,
    )
}
