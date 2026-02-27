package ccr.server.level.ldd

import ccr.math.Vector3
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroMatrix3D
import ccr.server.defs.readMicroSphere
import ccr.server.defs.readMicroVector3
import ccr.net.replication.NetworkObjectManager
import ccr.server.defs.BuildingGameObjDef
import ccr.server.defs.PhysDefClass
import ccr.server.defs.SimpleGameObjDef
import ccr.server.defs.VehicleGameObjDef
import ccr.server.level.ChunkIds
import ccr.server.level.DefinitionRegistry
import ccr.server.level.Matrix3D
import ccr.server.level.Sphere
import ccr.server.mix.ChunkReader
import ccr.server.net.BaseGameObj
import ccr.server.net.BuildingGameObj
import ccr.server.net.ComCenterGameObj
import ccr.server.net.PowerPlantGameObj
import ccr.server.net.RefineryGameObj
import ccr.server.net.SimpleGameObj
import ccr.server.net.SoldierFactoryGameObj
import ccr.server.net.VehicleFactoryGameObj
import ccr.server.net.VehicleGameObj
import ccr.server.net.WarFactoryGameObj

class GameObjectFactory(private val definitions: DefinitionRegistry) {

    // ─── Chunk ID constants (from C++ source) ──────────────────────────────────

    // BaseGameObj (basegameobj.cpp)
    private val BASEGAMEOBJ_CHUNKID_VARIABLES = 910991407u
    private val MICRO_DEFINITION_ID = 2  // MICROCHUNKID_DEFINITION_ID
    private val MICRO_INSTANCE_ID = 3    // MICROCHUNKID_INSTANCE_ID

    // DamageableGameObj (damageablegameobj.cpp)
    // enum: CHUNKID_PARENT=207011212, CHUNKID_DEFENSEOBJECT=207011213, CHUNKID_VARIABLES=207011214
    private val DAMAGEABLE_CHUNKID_VARIABLES   = 207011214u
    private val MICRO_DMG_PLAYER_TYPE          = 1
    private val MICRO_DMG_IS_HEALTH_BAR        = 2

    // DefenseObjectClass (damage.cpp)
    // enum: CHUNKID_VARIABLES=914991020
    // micro: WARHEAD=1,DAMAGE=2,HEALTH=3,HEALTH_MAX=4,SKIN=5,SHIELD_STRENGTH=6,
    //        SHIELD_STRENGTH_MAX=7,SHIELD_TYPE=8,XXX=9..12,DAMAGE_POINTS=13,DEATH_POINTS=14
    private val DEFENSE_CHUNKID_VARIABLES      = 914991020u
    private val MICRO_HEALTH                   = 3
    private val MICRO_HEALTH_MAX               = 4
    private val MICRO_SKIN                     = 5
    private val MICRO_SHIELD_STRENGTH          = 6
    private val MICRO_SHIELD_STRENGTH_MAX      = 7
    private val MICRO_SHIELD_TYPE              = 8
    private val MICRO_DAMAGE_POINTS            = 13
    private val MICRO_DEATH_POINTS             = 14

    // BuildingGameObj (building.cpp)
    private val BUILDING_CHUNKID_VARIABLES     = 207011121u
    private val MICRO_BUILDING_POSITION        = 1
    private val MICRO_BUILDING_ISPOWERON       = 2
    private val MICRO_BUILDING_COLLECTION_SPHERE = 3

    // ─── Public entry point ─────────────────────────────────────────────────────

    /**
     * Creates a real [BaseGameObj] from the LDD chunk data for the given factory chunk ID.
     * Returns null for object types not yet supported (soldiers, zones, C4, etc. — spawned/handled elsewhere).
     * The created object is registered with [NetworkObjectManager] at its LDD-assigned [networkId].
     */
    fun load(factoryChunkId: UInt, objDataReader: ChunkReader): BaseGameObj? {
        val (defId, networkId) = extractBaseFields(objDataReader)

        return when {
            ChunkIds.isBuilding(factoryChunkId) ->
                extractBuildingGameObj(objDataReader, defId, networkId, factoryChunkId)
            factoryChunkId == ChunkIds.GAMEOBJ_VEHICLE ->
                extractVehicleGameObj(objDataReader, defId, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SIMPLE ->
                extractSimpleGameObj(objDataReader, defId, networkId)
            else -> null  // soldiers (spawned by god), script zones, C4, beacons, etc. — not created here
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

    /** DefenseObjectClass instance state extracted from DEFENSE_CHUNKID_VARIABLES (914991020). */
    private data class DefenseFields(
        val health: Float = 0f,
        val healthMax: Float = 0f,
        val skinSaveId: Int = 0,
        val shieldStrength: Float = 0f,
        val shieldStrengthMax: Float = 0f,
        val shieldTypeSaveId: Int = 0,
        val damagePoints: Float = 0f,
        val deathPoints: Float = 0f,
    )

    /**
     * Extracts DefenseObjectClass instance state.
     * DEFENSE_CHUNKID_VARIABLES (914991020): micros 3..14.
     */
    private fun extractDefenseObject(objDataReader: ChunkReader): DefenseFields {
        val chunk = objDataReader.findChunkRecursive(DEFENSE_CHUNKID_VARIABLES)
            ?: return DefenseFields()
        return DefenseFields(
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

    // ─── Model name resolution ─────────────────────────────────────────────────

    /**
     * Resolves the W3D model base name from a PhysDefClass definition ID.
     * Strips leading path components and file extension.
     * C++: Peek_Physical_Object()->Peek_Model()->Get_Name() returns the HLod chunk name.
     * FIXME: replace with W3D HLod name lookup once W3D loading is implemented.
     */
    private fun resolvePhysDefModelName(physDefId: Int): String {
        val raw = (definitions.findById(physDefId.toUInt()) as? PhysDefClass)?.modelName ?: return ""
        return raw.substringAfterLast('\\').substringAfterLast('/').substringBeforeLast('.')
    }

    // ─── Per-type extractors ────────────────────────────────────────────────────

    private fun extractBuildingGameObj(
        reader: ChunkReader, defId: Int, networkId: Int, factoryChunkId: UInt,
    ): BuildingGameObj? {
        if (!ChunkIds.isBuilding(factoryChunkId)) return null

        val dmg = extractDamageableFields(reader)
        val defense = extractDefenseObject(reader)

        val buildingVars = reader.findChunkRecursive(BUILDING_CHUNKID_VARIABLES)
        // readMicroVector3 returns ccr.math.Vector3
        val pos = buildingVars?.readMicroVector3(MICRO_BUILDING_POSITION) ?: Vector3(0f, 0f, 0f)
        val isPowerOn = buildingVars?.readMicroBool(MICRO_BUILDING_ISPOWERON) ?: true
        // readMicroSphere returns ccr.server.level.Sphere(ccr.math.Vector3 center, radius)
        var sphere = buildingVars?.readMicroSphere(MICRO_BUILDING_COLLECTION_SPHERE)
            ?: Sphere(Vector3(0f, 0f, 0f), 50f)

        // C++ post-load fixup: if sphere center is (0,0,0), copy position into it
        if (sphere.center.x == 0f && sphere.center.y == 0f && sphere.center.z == 0f) {
            sphere = Sphere(pos, sphere.radius)
        }

        val building: BuildingGameObj = when (factoryChunkId) {
            ChunkIds.GAMEOBJ_BUILDING_POWERPLANT    -> PowerPlantGameObj()
            ChunkIds.GAMEOBJ_BUILDING_REFINERY      -> RefineryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY -> SoldierFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_WARFACTORY    -> WarFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP,
            ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY -> VehicleFactoryGameObj()
            ChunkIds.GAMEOBJ_BUILDING_COMCENTER     -> ComCenterGameObj()
            else                                    -> BuildingGameObj()
        }

        // Apply definition (loads mctSkin, etc.)
        val def = definitions.findById(defId.toUInt()) as? BuildingGameObjDef
        if (def != null) building.init(def)

        // Set position (also updates collectionSphere.center)
        building.setPosition(pos)
        building.collectionSphere = sphere

        // Defence state from LDD
        val healthMax = defense.healthMax.takeIf { it > 0f } ?: 5000f
        building.health    = healthMax
        building.healthMax = healthMax
        building.shieldType = defense.skinSaveId
        if (defense.shieldStrength > 0f) building.shieldStrength = defense.shieldStrength

        building.isPowerOn  = isPowerOn
        building.playerType = dmg.playerType

        // Register with NetworkObjectManager at the LDD-assigned network ID (only if non-zero)
        if (networkId != 0) NetworkObjectManager.registerObject(building, networkId)

        return building
    }

    private fun extractVehicleGameObj(reader: ChunkReader, defId: Int, networkId: Int): VehicleGameObj? {
        val dmg = extractDamageableFields(reader)

        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        // Matrix3D.position returns ccr.server.level.Vector3; convert to ccr.math.Vector3
        val levelPos = transform.position
        val pos = Vector3(levelPos.x, levelPos.y, levelPos.z)

        val wrapper = definitions.findById(defId.toUInt()) as? VehicleGameObjDef
        val seatCount = wrapper?.numSeats ?: 2
        val physDefId = wrapper?.physical?.physDefId ?: 0
        // C++: Peek_Physical_Object()->Peek_Model()->Get_Name() → uppercase for vehicles
        val modelName = resolvePhysDefModelName(physDefId).uppercase()

        val vehicle = VehicleGameObj()
        if (wrapper != null) vehicle.definition = wrapper
        vehicle.modelName        = modelName
        vehicle.position         = pos
        vehicle.playerType       = dmg.playerType
        vehicle.vehicleDelivered = true
        vehicle.controlOwner     = 0
        vehicle.seatOccupants.clear()
        repeat(seatCount) { vehicle.seatOccupants.add(null) }

        // Register with NetworkObjectManager at the LDD-assigned network ID (only if non-zero)
        if (networkId != 0) NetworkObjectManager.registerObject(vehicle, networkId)

        return vehicle
    }

    private fun extractSimpleGameObj(reader: ChunkReader, defId: Int, networkId: Int): SimpleGameObj? {
        val physChunk = reader.findChunkRecursive(910991146u)
        val transform = physChunk?.readMicroMatrix3D(1) ?: Matrix3D.IDENTITY
        // Matrix3D.position returns ccr.server.level.Vector3; convert to ccr.math.Vector3
        val levelPos = transform.position
        val pos = Vector3(levelPos.x, levelPos.y, levelPos.z)

        // Resolve model name via PhysDefClass chain (no uppercase for simple objects)
        val wrapper = definitions.findById(defId.toUInt()) as? SimpleGameObjDef
        val physDefId = wrapper?.physDefId ?: 0
        val modelName = if (physDefId != 0) resolvePhysDefModelName(physDefId) else ""

        val obj = SimpleGameObj()
        if (wrapper != null) obj.definition = wrapper
        obj.modelName = modelName
        obj.position  = pos

        // Register with NetworkObjectManager at the LDD-assigned network ID (only if non-zero)
        if (networkId != 0) NetworkObjectManager.registerObject(obj, networkId)

        return obj
    }
}
