package ccr.server.level.ldd

import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroMatrix3D
import ccr.server.defs.readMicroOBBox
import ccr.server.defs.readMicroSphere
import ccr.server.defs.readMicroString
import ccr.server.defs.readMicroVector3
import ccr.server.level.ChunkIds
import ccr.server.level.Matrix3D
import ccr.server.level.OBBox
import ccr.server.level.Sphere
import ccr.server.level.Vector3
import ccr.server.level.toFloat32
import ccr.server.level.toInt32
import ccr.server.mix.ChunkReader

object GameObjectFactory {

    // ─── Chunk ID constants (from C++ source) ──────────────────────────────────

    // BaseGameObj (basegameobj.cpp)
    private const val BASEGAMEOBJ_CHUNKID_VARIABLES = 910991407u
    private const val MICRO_DEFINITION_ID = 2  // MICROCHUNKID_DEFINITION_ID
    private const val MICRO_INSTANCE_ID = 3    // MICROCHUNKID_INSTANCE_ID

    // DamageableGameObj (damageablegameobj.cpp)
    // enum: CHUNKID_PARENT=207011212, CHUNKID_DEFENSEOBJECT=207011213, CHUNKID_VARIABLES=207011214
    private const val DAMAGEABLE_CHUNKID_VARIABLES   = 207011214u
    private const val MICRO_DMG_PLAYER_TYPE          = 1
    private const val MICRO_DMG_IS_HEALTH_BAR        = 2

    // DefenseObjectClass (damage.cpp)
    // enum: CHUNKID_VARIABLES=914991020
    // micro: WARHEAD=1,DAMAGE=2,HEALTH=3,HEALTH_MAX=4,SKIN=5,SHIELD_STRENGTH=6,
    //        SHIELD_STRENGTH_MAX=7,SHIELD_TYPE=8,XXX=9..12,DAMAGE_POINTS=13,DEATH_POINTS=14
    private const val DEFENSE_CHUNKID_VARIABLES      = 914991020u
    private const val MICRO_HEALTH                   = 3
    private const val MICRO_HEALTH_MAX               = 4
    private const val MICRO_SKIN                     = 5
    private const val MICRO_SHIELD_STRENGTH          = 6
    private const val MICRO_SHIELD_STRENGTH_MAX      = 7
    private const val MICRO_SHIELD_TYPE              = 8
    private const val MICRO_DAMAGE_POINTS            = 13
    private const val MICRO_DEATH_POINTS             = 14

    // ArmedGameObj (armedgameobj.cpp)
    private const val ARMED_CHUNKID_VARIABLES        = 418001842u
    private const val ARMED_CHUNKID_WEAPONBAG        = 418001843u
    private const val MICRO_TARGETING_POS            = 1

    // Weapon bag entry micros (armedgameobj.cpp, per-weapon chunk)
    private const val MICRO_WEAPON_DEF_ID            = 1
    private const val MICRO_WEAPON_ROUNDS            = 2
    private const val MICRO_WEAPON_INV_ROUNDS        = 3

    // SmartGameObj (smartgameobj.cpp)
    private const val SMART_CHUNKID_CONTROL          = 910991115u
    private const val MICRO_CONTROL_ENABLED          = 1

    // BuildingGameObj (building.cpp)
    private const val BUILDING_CHUNKID_VARIABLES     = 207011121u
    private const val MICRO_BUILDING_POSITION        = 1
    private const val MICRO_BUILDING_ISPOWERON       = 2
    private const val MICRO_BUILDING_COLLECTION_SPHERE = 3

    // SoldierGameObj (soldier.cpp)
    private const val SOLDIER_CHUNKID_VARIABLES      = 909991657u
    private const val SOLDIER_CHUNKID_HUMAN_STATE    = 909991659u
    private const val MICRO_SOLDIER_DETONATE_C4      = 1
    private const val MICRO_SOLDIER_LEG_FACING       = 2
    private const val MICRO_SOLDIER_SYNC_LEGS        = 3
    private const val MICRO_SOLDIER_ANIM_NAME        = 4
    private const val MICRO_SOLDIER_VEHICLE_ID       = 5
    private const val MICRO_SOLDIER_INNATE_ENABLE    = 8
    private const val MICRO_SOLDIER_LAST_LEG_MODE    = 10
    private const val MICRO_SOLDIER_HEAD_LOOK_DUR    = 11
    private const val MICRO_SOLDIER_HEAD_ROTATION    = 12
    private const val MICRO_SOLDIER_LOOK_TARGET      = 13
    private const val MICRO_SOLDIER_KEY_RING         = 15
    private const val MICRO_SOLDIER_AI_STATE         = 16
    private const val MICRO_SOLDIER_LOOK_ANGLE       = 18
    private const val MICRO_SOLDIER_LOOK_ANGLE_TIMER = 19
    private const val MICRO_SOLDIER_SPECIAL_DMG_MODE = 22
    private const val MICRO_SOLDIER_SPECIAL_DMG_TIMER = 23
    private const val MICRO_SOLDIER_GHOST_COLLISION  = 24
    private const val MICRO_HUMAN_STATE              = 1   // inside SOLDIER_CHUNKID_HUMAN_STATE

    // VehicleGameObj (vehicle.cpp)
    private const val VEHICLE_CHUNKID_VARIABLES      = 923991633u
    private const val MICRO_VEHICLE_TURRET_TURN      = 5
    private const val MICRO_VEHICLE_BARREL_TILT      = 6
    private const val MICRO_VEHICLE_TRANSITIONS      = 7
    private const val MICRO_VEHICLE_OCCUPIED_SEATS   = 8
    private const val MICRO_VEHICLE_NUM_SEATS        = 9

    // ScriptZoneGameObj (scriptzone.cpp)
    // enum: CHUNKID_PARENT_OLD=922991806, CHUNKID_VARIABLES=922991807
    private const val SCRIPTZONE_CHUNKID_VARIABLES   = 922991807u
    private const val MICRO_ZONE_BOUNDING_BOX        = 1
    private const val MICRO_ZONE_PLAYER_TYPE         = 2

    // C4GameObj (c4.cpp) — also used by SAMSiteGameObj
    private const val C4_CHUNKID_VARIABLES           = 922991751u
    private const val MICRO_C4_AMMO_DEF_ID           = 3
    private const val MICRO_C4_DETONATION_MODE       = 4
    private const val MICRO_C4_TIMER                 = 5
    private const val MICRO_C4_STUCK                 = 6
    private const val MICRO_C4_STUCK_OFFSET          = 7
    private const val MICRO_C4_STUCK_MCT             = 8
    private const val MICRO_C4_STUCK_BONE            = 9
    private const val MICRO_C4_STUCK_STATIC_ID       = 10
    private const val MICRO_C4_STUCK_TO_OBJECT       = 11
    private const val MICRO_C4_AGE                   = 12
    private const val MICRO_SAM_STATE                = 2
    private const val MICRO_SAM_TIMER                = 3

    // BeaconGameObj (beacongameobj.cpp)
    private const val BEACON_CHUNKID_VARIABLES       = 0x00219044u
    private const val MICRO_BEACON_STATE             = 1
    private const val MICRO_BEACON_STATE_TIMER       = 2
    private const val MICRO_BEACON_DETONATE_TIMER    = 3
    private const val MICRO_BEACON_PREDETONATE_TIMER = 4
    private const val MICRO_BEACON_IS_ARMED          = 5

    // PowerUpGameObj (powerup.cpp)
    private const val POWERUP_CHUNKID_VARIABLES      = 927991636u
    private const val MICRO_POWERUP_STATE            = 1
    private const val MICRO_POWERUP_STATE_END_TIMER  = 2

    // TransitionGameObj (transitiongameobj.cpp)
    private const val TRANSITION_CHUNKID_VARIABLES   = 1111991207u
    private const val MICRO_TRANSITION_TRANSFORM     = 1
    private const val MICRO_TRANSITION_LADDER_INDEX  = 2

    // SpecialEffectsGameObj (specialeffectsgameobj.cpp)
    private const val SPECIALEFFECTS_CHUNKID_VARIABLES = 0x09010237u
    private const val MICRO_SFXOBJ_LIFE_REMAINING    = 1
    private const val MICRO_SFXOBJ_IS_INITIALIZED    = 2

    // SakuraBossGameObj (sakurabossgameobj.cpp)
    // enum: CHUNKID_PARENT=0x09070459, CHUNKID_VARIABLES=0x0907045A
    private const val SAKURA_CHUNKID_VARIABLES       = 0x0907045Au
    private const val MICRO_SAKURA_AVAILABLE_WEAPONS = 1
    private const val MICRO_SAKURA_OVERALL_STATE     = 2
    private const val MICRO_SAKURA_NEXT_STATE        = 3
    private const val MICRO_SAKURA_FACE_TARGET       = 4
    private const val MICRO_SAKURA_ROCKET_STATE      = 5
    private const val MICRO_SAKURA_GATTLING_STATE    = 6

    // DamageZoneGameObj (damagezone.cpp)
    private const val DAMAGEZONE_CHUNKID_VARIABLES   = 626000948u
    private const val MICRO_DAMAGEZONE_BOUNDING_BOX  = 1
    private const val MICRO_DAMAGEZONE_DAMAGE_TIMER  = 2

    // ─── Public entry point ─────────────────────────────────────────────────────

    fun load(factoryChunkId: UInt, objDataReader: ChunkReader): LoadedGameObject {
        val (defId, networkId) = extractBaseFields(objDataReader)

        return when {
            ChunkIds.isBuilding(factoryChunkId) ->
                extractBuildingGameObj(objDataReader, defId, networkId, factoryChunkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SOLDIER ->
                extractSoldierGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_VEHICLE ->
                extractVehicleGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SIMPLE ->
                extractSimpleGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SCRIPTZONE ->
                extractScriptZoneGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_C4 ->
                extractC4GameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_BEACON ->
                extractBeaconGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_POWERUP ->
                extractPowerUpGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_TRANSITION ->
                extractTransitionGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_CINEMATIC ->
                extractCinematicGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_DAMAGEZONE ->
                extractDamageZoneGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SPECIALEFFECTS ->
                extractSpecialEffectsGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SAMSITE ->
                extractSAMSiteGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SAKURA_BOSS ->
                extractSakuraBossGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_MENDOZA_BOSS ->
                extractMendozaBossGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_RAVESHAW_BOSS ->
                extractRaveshawBossGameObj(objDataReader, defId, networkId)
            else -> UnknownGameObj(defId, Matrix3D.IDENTITY, networkId, factoryChunkId)
        }
    }

    // ─── Base field extraction ──────────────────────────────────────────────────

    private data class BaseFields(val defId: Int, val networkId: Int)

    /**
     * Extracts definitionId and instanceId from the BaseGameObj save hierarchy.
     * BASEGAMEOBJ_CHUNKID_VARIABLES (910991407): micro 2 = defId, micro 3 = networkId.
     */
    private fun extractBaseFields(objDataReader: ChunkReader): BaseFields {
        val baseVarsChunk = objDataReader.findChunkRecursive(BASEGAMEOBJ_CHUNKID_VARIABLES)
            ?: return BaseFields(0, 0)
        val defId = baseVarsChunk.readMicroInt(MICRO_DEFINITION_ID) ?: 0
        val networkId = baseVarsChunk.readMicroInt(MICRO_INSTANCE_ID) ?: 0
        return BaseFields(defId, networkId)
    }

    /**
     * Extracts DamageableGameObj instance fields.
     * DAMAGEABLE_CHUNKID_VARIABLES (207011214): micro 1 = playerType, micro 2 = isHealthBarDisplayed.
     */
    private data class DamageableFields(val playerType: Int, val isHealthBarDisplayed: Boolean)

    private fun extractDamageableFields(objDataReader: ChunkReader): DamageableFields {
        val chunk = objDataReader.findChunkRecursive(DAMAGEABLE_CHUNKID_VARIABLES)
            ?: return DamageableFields(-2, true)
        return DamageableFields(
            playerType = chunk.readMicroInt(MICRO_DMG_PLAYER_TYPE) ?: -2,
            isHealthBarDisplayed = chunk.readMicroBool(MICRO_DMG_IS_HEALTH_BAR) ?: true,
        )
    }

    /**
     * Extracts DefenseObjectClass instance state.
     * DEFENSE_CHUNKID_VARIABLES (914991020): micros 3..14.
     */
    private fun extractDefenseObject(objDataReader: ChunkReader): LoadedDefenseObject {
        val chunk = objDataReader.findChunkRecursive(DEFENSE_CHUNKID_VARIABLES)
            ?: return LoadedDefenseObject()
        return LoadedDefenseObject(
            health = chunk.readMicroFloat(MICRO_HEALTH) ?: 0f,
            healthMax = chunk.readMicroFloat(MICRO_HEALTH_MAX) ?: 0f,
            skinSaveId = chunk.readMicroInt(MICRO_SKIN) ?: 0,
            shieldStrength = chunk.readMicroFloat(MICRO_SHIELD_STRENGTH) ?: 0f,
            shieldStrengthMax = chunk.readMicroFloat(MICRO_SHIELD_STRENGTH_MAX) ?: 0f,
            shieldTypeSaveId = chunk.readMicroInt(MICRO_SHIELD_TYPE) ?: 0,
            damagePoints = chunk.readMicroFloat(MICRO_DAMAGE_POINTS) ?: 0f,
            deathPoints = chunk.readMicroFloat(MICRO_DEATH_POINTS) ?: 0f,
        )
    }

    /**
     * Extracts ArmedGameObj targeting position.
     * ARMED_CHUNKID_VARIABLES (418001842): micro 1 = targetingPosition (Vector3).
     */
    private fun extractTargetingPosition(objDataReader: ChunkReader): Vector3 {
        val chunk = objDataReader.findChunkRecursive(ARMED_CHUNKID_VARIABLES)
            ?: return Vector3(0f, 0f, 0f)
        return chunk.readMicroVector3(MICRO_TARGETING_POS) ?: Vector3(0f, 0f, 0f)
    }

    /**
     * Extracts weapon bag entries from ARMED_CHUNKID_WEAPONBAG (418001843).
     * Each entry chunk contains micro 1=defId, 2=rounds, 3=inventoryRounds.
     */
    private fun extractWeaponBag(objDataReader: ChunkReader): List<LoadedWeaponBagEntry> {
        val bagChunk = objDataReader.findChunkRecursive(ARMED_CHUNKID_WEAPONBAG)
            ?: return emptyList()
        val entries = mutableListOf<LoadedWeaponBagEntry>()
        bagChunk.forEachChunk { _, _, entryReader ->
            val defId = entryReader.readMicroInt(MICRO_WEAPON_DEF_ID) ?: 0
            val rounds = entryReader.readMicroInt(MICRO_WEAPON_ROUNDS) ?: 0
            val invRounds = entryReader.readMicroInt(MICRO_WEAPON_INV_ROUNDS) ?: 0
            entries += LoadedWeaponBagEntry(defId, rounds, invRounds)
        }
        return entries
    }

    // ─── Per-type extractors ────────────────────────────────────────────────────

    private fun extractBuildingGameObj(
        reader: ChunkReader, defId: Int, networkId: Int, factoryChunkId: UInt,
    ): LoadedBuildingGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)

        val buildingVars = reader.findChunkRecursive(BUILDING_CHUNKID_VARIABLES)
        val position = buildingVars?.readMicroVector3(MICRO_BUILDING_POSITION) ?: Vector3(0f, 0f, 0f)
        val isPowerOn = buildingVars?.readMicroBool(MICRO_BUILDING_ISPOWERON) ?: true
        var sphere = buildingVars?.readMicroSphere(MICRO_BUILDING_COLLECTION_SPHERE)
            ?: Sphere(Vector3(0f, 0f, 0f), 50f)

        // C++ post-load fixup: if sphere center is (0,0,0), copy position into it
        if (sphere.center.x == 0f && sphere.center.y == 0f && sphere.center.z == 0f) {
            sphere = sphere.copy(center = position)
        }

        val transform = Matrix3D(floatArrayOf(
            1f, 0f, 0f, position.x,
            0f, 1f, 0f, position.y,
            0f, 0f, 1f, position.z,
        ))

        return LoadedBuildingGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            isPowerOn = isPowerOn,
            collectionSphere = sphere,
            factoryChunkId = factoryChunkId,
        )
    }

    private fun extractSoldierGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedSoldierGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val weaponBag = extractWeaponBag(reader)
        val targetingPos = extractTargetingPosition(reader)

        val physChunk = reader.findChunkRecursive(910991146u)  // PhysicalGameObj CHUNKID_VARIABLES
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY

        val soldierVars = reader.findChunkRecursive(SOLDIER_CHUNKID_VARIABLES)
        val humanStateChunk = reader.findChunkRecursive(SOLDIER_CHUNKID_HUMAN_STATE)
        val smartControlChunk = reader.findChunkRecursive(SMART_CHUNKID_CONTROL)

        return LoadedSoldierGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            detonateC4 = soldierVars?.readMicroBool(MICRO_SOLDIER_DETONATE_C4) ?: false,
            legFacing = soldierVars?.readMicroFloat(MICRO_SOLDIER_LEG_FACING) ?: 0f,
            syncLegs = soldierVars?.readMicroBool(MICRO_SOLDIER_SYNC_LEGS) ?: true,
            animName = soldierVars?.readMicroString(MICRO_SOLDIER_ANIM_NAME) ?: "",
            vehicleId = soldierVars?.readMicroInt(MICRO_SOLDIER_VEHICLE_ID) ?: 0,
            innateEnableBits = soldierVars?.readMicroInt(MICRO_SOLDIER_INNATE_ENABLE) ?: 0xFF,
            lastLegMode = soldierVars?.readMicroInt(MICRO_SOLDIER_LAST_LEG_MODE) ?: 0,
            headLookDuration = soldierVars?.readMicroFloat(MICRO_SOLDIER_HEAD_LOOK_DUR) ?: 0f,
            headRotation = soldierVars?.readMicroFloat(MICRO_SOLDIER_HEAD_ROTATION) ?: 0f,
            lookTarget = soldierVars?.readMicroVector3(MICRO_SOLDIER_LOOK_TARGET) ?: Vector3(0f, 0f, 0f),
            keyRing = soldierVars?.readMicroInt(MICRO_SOLDIER_KEY_RING) ?: 0,
            aiState = soldierVars?.readMicroInt(MICRO_SOLDIER_AI_STATE) ?: 0,
            lookAngle = soldierVars?.readMicroFloat(MICRO_SOLDIER_LOOK_ANGLE) ?: 0f,
            lookAngleTimer = soldierVars?.readMicroFloat(MICRO_SOLDIER_LOOK_ANGLE_TIMER) ?: 0f,
            specialDamageMode = soldierVars?.readMicroInt(MICRO_SOLDIER_SPECIAL_DMG_MODE) ?: 0,
            specialDamageTimer = soldierVars?.readMicroFloat(MICRO_SOLDIER_SPECIAL_DMG_TIMER) ?: 0f,
            isUsingGhostCollision = soldierVars?.readMicroBool(MICRO_SOLDIER_GHOST_COLLISION) ?: false,
            humanState = humanStateChunk?.readMicroInt(MICRO_HUMAN_STATE) ?: 0,
            weaponBag = weaponBag,
            targetingPosition = targetingPos,
            controlEnabled = smartControlChunk?.readMicroBool(MICRO_CONTROL_ENABLED) ?: true,
        )
    }

    private fun extractVehicleGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedVehicleGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val weaponBag = extractWeaponBag(reader)
        val targetingPos = extractTargetingPosition(reader)

        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY

        val vehicleVars = reader.findChunkRecursive(VEHICLE_CHUNKID_VARIABLES)
        return LoadedVehicleGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            turretTurn = vehicleVars?.readMicroFloat(MICRO_VEHICLE_TURRET_TURN) ?: 0f,
            barrelTilt = vehicleVars?.readMicroFloat(MICRO_VEHICLE_BARREL_TILT) ?: 0f,
            transitionsEnabled = vehicleVars?.readMicroBool(MICRO_VEHICLE_TRANSITIONS) ?: true,
            occupiedSeats = vehicleVars?.readMicroInt(MICRO_VEHICLE_OCCUPIED_SEATS) ?: 0,
            numSeats = vehicleVars?.readMicroInt(MICRO_VEHICLE_NUM_SEATS) ?: 0,
            weaponBag = weaponBag,
            targetingPosition = targetingPos,
        )
    }

    private fun extractSimpleGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedSimpleGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        return LoadedSimpleGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            defense = defense,
        )
    }

    private fun extractScriptZoneGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedScriptZoneGameObj {
        val varsChunk = reader.findChunkRecursive(SCRIPTZONE_CHUNKID_VARIABLES)
        val boundingBox = varsChunk?.readMicroOBBox(MICRO_ZONE_BOUNDING_BOX) ?: OBBox.EMPTY

        // Legacy playerType remapping (C++ combatchunkid.h):
        // 2 → -2 (neutral), 3 → -2, 4 → 0 (NOD), 5 → 1 (GDI)
        val rawPlayerType = varsChunk?.readMicroInt(MICRO_ZONE_PLAYER_TYPE) ?: -2
        val playerType = when (rawPlayerType) {
            2, 3 -> -2
            4 -> 0
            5 -> 1
            else -> rawPlayerType
        }

        return LoadedScriptZoneGameObj(
            definitionId = defId,
            transform = Matrix3D.IDENTITY,
            networkId = networkId,
            boundingBox = boundingBox,
            playerType = playerType,
        )
    }

    private fun extractC4GameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedC4GameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val c4Vars = reader.findChunkRecursive(C4_CHUNKID_VARIABLES)
        return LoadedC4GameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            ammoDefId = c4Vars?.readMicroInt(MICRO_C4_AMMO_DEF_ID) ?: 0,
            detonationMode = c4Vars?.readMicroInt(MICRO_C4_DETONATION_MODE) ?: 0,
            timer = c4Vars?.readMicroFloat(MICRO_C4_TIMER) ?: 0f,
            stuck = c4Vars?.readMicroBool(MICRO_C4_STUCK) ?: false,
            stuckOffset = c4Vars?.readMicroVector3(MICRO_C4_STUCK_OFFSET) ?: Vector3(0f, 0f, 0f),
            stuckMct = c4Vars?.readMicroBool(MICRO_C4_STUCK_MCT) ?: false,
            stuckBone = c4Vars?.readMicroString(MICRO_C4_STUCK_BONE) ?: "",
            stuckStaticAnimObjId = c4Vars?.readMicroInt(MICRO_C4_STUCK_STATIC_ID) ?: 0,
            stuckToObject = c4Vars?.readMicroInt(MICRO_C4_STUCK_TO_OBJECT) ?: 0,
            age = c4Vars?.readMicroFloat(MICRO_C4_AGE) ?: 0f,
        )
    }

    private fun extractBeaconGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedBeaconGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val beaconVars = reader.findChunkRecursive(BEACON_CHUNKID_VARIABLES)
        return LoadedBeaconGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            state = beaconVars?.readMicroInt(MICRO_BEACON_STATE) ?: 0,
            stateTimer = beaconVars?.readMicroFloat(MICRO_BEACON_STATE_TIMER) ?: 0f,
            detonateTimer = beaconVars?.readMicroFloat(MICRO_BEACON_DETONATE_TIMER) ?: 0f,
            preDetonateTimer = beaconVars?.readMicroFloat(MICRO_BEACON_PREDETONATE_TIMER) ?: 0f,
            isArmed = beaconVars?.readMicroBool(MICRO_BEACON_IS_ARMED) ?: false,
        )
    }

    private fun extractPowerUpGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedPowerUpGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val powerUpVars = reader.findChunkRecursive(POWERUP_CHUNKID_VARIABLES)
        return LoadedPowerUpGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            state = powerUpVars?.readMicroInt(MICRO_POWERUP_STATE) ?: 0,
            stateEndTimer = powerUpVars?.readMicroFloat(MICRO_POWERUP_STATE_END_TIMER) ?: 0f,
        )
    }

    private fun extractTransitionGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedTransitionGameObj {
        val transVars = reader.findChunkRecursive(TRANSITION_CHUNKID_VARIABLES)
        val transform = transVars?.readMicroMatrix3D(MICRO_TRANSITION_TRANSFORM) ?: Matrix3D.IDENTITY
        val ladderIndex = transVars?.readMicroInt(MICRO_TRANSITION_LADDER_INDEX) ?: 0
        return LoadedTransitionGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            ladderIndex = ladderIndex,
        )
    }

    private fun extractCinematicGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedCinematicGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        return LoadedCinematicGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
        )
    }

    private fun extractDamageZoneGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedDamageZoneGameObj {
        val varsChunk = reader.findChunkRecursive(DAMAGEZONE_CHUNKID_VARIABLES)
        return LoadedDamageZoneGameObj(
            definitionId = defId,
            transform = Matrix3D.IDENTITY,
            networkId = networkId,
            boundingBox = varsChunk?.readMicroOBBox(MICRO_DAMAGEZONE_BOUNDING_BOX) ?: OBBox.EMPTY,
            damageTimer = varsChunk?.readMicroFloat(MICRO_DAMAGEZONE_DAMAGE_TIMER) ?: 0f,
        )
    }

    private fun extractSpecialEffectsGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedSpecialEffectsGameObj {
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val sfxVars = reader.findChunkRecursive(SPECIALEFFECTS_CHUNKID_VARIABLES)
        return LoadedSpecialEffectsGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            lifeRemaining = sfxVars?.readMicroFloat(MICRO_SFXOBJ_LIFE_REMAINING) ?: 0f,
            isInitialized = sfxVars?.readMicroBool(MICRO_SFXOBJ_IS_INITIALIZED) ?: false,
        )
    }

    private fun extractSAMSiteGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedSAMSiteGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val samVars = reader.findChunkRecursive(C4_CHUNKID_VARIABLES)
        return LoadedSAMSiteGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            state = samVars?.readMicroInt(MICRO_SAM_STATE) ?: 0,
            timer = samVars?.readMicroFloat(MICRO_SAM_TIMER) ?: 0f,
        )
    }

    private fun extractSakuraBossGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedSakuraBossGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val weaponBag = extractWeaponBag(reader)
        val targetingPos = extractTargetingPosition(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        val sakuraVars = reader.findChunkRecursive(SAKURA_CHUNKID_VARIABLES)
        return LoadedSakuraBossGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            weaponBag = weaponBag,
            targetingPosition = targetingPos,
            availableWeapons = sakuraVars?.readMicroInt(MICRO_SAKURA_AVAILABLE_WEAPONS) ?: 0,
            overallState = sakuraVars?.readMicroInt(MICRO_SAKURA_OVERALL_STATE) ?: 0,
            nextOverallState = sakuraVars?.readMicroInt(MICRO_SAKURA_NEXT_STATE) ?: 0,
            faceTargetInTransition = sakuraVars?.readMicroBool(MICRO_SAKURA_FACE_TARGET) ?: false,
            rocketLauncherState = sakuraVars?.readMicroInt(MICRO_SAKURA_ROCKET_STATE) ?: 0,
            gattlingGunState = sakuraVars?.readMicroInt(MICRO_SAKURA_GATTLING_STATE) ?: 0,
        )
    }

    private fun extractMendozaBossGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedMendozaBossGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val weaponBag = extractWeaponBag(reader)
        val targetingPos = extractTargetingPosition(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        return LoadedMendozaBossGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            weaponBag = weaponBag,
            targetingPosition = targetingPos,
        )
    }

    private fun extractRaveshawBossGameObj(reader: ChunkReader, defId: Int, networkId: Int): LoadedRaveshawBossGameObj {
        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)
        val weaponBag = extractWeaponBag(reader)
        val targetingPos = extractTargetingPosition(reader)
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        return LoadedRaveshawBossGameObj(
            definitionId = defId,
            transform = transform,
            networkId = networkId,
            playerType = dmg.playerType,
            isHealthBarDisplayed = dmg.isHealthBarDisplayed,
            defense = defense,
            weaponBag = weaponBag,
            targetingPosition = targetingPos,
        )
    }
}
