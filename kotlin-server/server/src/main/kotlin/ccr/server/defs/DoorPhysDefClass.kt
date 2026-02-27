package ccr.server.defs

import ccr.server.defs.phys.AnimCollisionManagerDef
import ccr.server.defs.phys.ProjectorManagerDef
import ccr.server.defs.phys.parseStaticAnimPhysDefClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of C++ DoorPhysDefClass (Combat/doors.cpp).
 *
 * C++ hierarchy: DoorPhysDefClass : AccessiblePhysDefClass : StaticAnimPhysDefClass :
 *   StaticPhysDefClass : PhysDefClass : DefinitionClass
 *
 * C++ defaults (doors.cpp constructor):
 *   CloseDelay(2), TriggerZone1(center(0,0,0),extent(1,1,1)),
 *   TriggerZone2(center(0,0,0),extent(1,1,1)), OpenSoundDefID(0),
 *   CloseSoundDefID(0), UnlockSoundDefID(0), AccessDeniedSoundDefID(0),
 *   DoorOpensForVehicles(false)
 *
 * Chunk layout inside OBJDATA:
 *   [320001904] parent (AccessiblePhysDefClass::Save)
 *     [0x10311249] parent (StaticAnimPhysDefClass::Save)
 *       [0x55110100] parent (StaticPhysDefClass::Save)
 *         [0x01070002] parent (PhysDefClass::Save)
 *           [0x055FFE07] DefinitionClass (name, id)
 *           [0x055FFE08] PhysDef variables (modelName, isPreLit)
 *         [0x01070003] StaticPhysDef variables (isNonOccluder)
 *       [0x55110101] ProjectorManagerDef
 *       [0x55110102] StaticAnimPhysDef variables
 *       [0x55110103] AnimCollisionManagerDef
 *     [0x1031124A] Accessible variables (lockCode)
 *   [320001903] door-specific variables
 */
class DoorPhysDefClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    // PhysDefClass
    val modelName: String = "NULL",
    val isPreLit: Boolean = false,
    // StaticPhysDefClass
    val isNonOccluder: Boolean = true,
    // StaticAnimPhysDefClass
    val shadowDynamicObjs: Boolean = false,
    val shadowIsAdditive: Boolean = false,
    val shadowIgnoresZRotation: Boolean = true,
    val shadowNearZ: Float = 0.5f,
    val shadowFarZ: Float = 5.0f,
    val shadowIntensity: Float = 0.5f,
    val doesCollideInPathfind: Boolean = false,
    val isCosmetic: Boolean = false,
    val animManagerDef: AnimCollisionManagerDef = AnimCollisionManagerDef(),
    val projectorManagerDef: ProjectorManagerDef = ProjectorManagerDef(),
    // AccessiblePhysDefClass
    val lockCode: Int = 0,
    // DoorPhysDefClass own fields
    val closeDelay: Float = 2f,
    /** OBBoxClass: 15 floats (basis 9f + center 3f + extent 3f). */
    val triggerZone1: FloatArray? = null,
    /** OBBoxClass: 15 floats (basis 9f + center 3f + extent 3f). */
    val triggerZone2: FloatArray? = null,
    val openSoundDefId: Int = 0,
    val closeSoundDefId: Int = 0,
    val unlockSoundDefId: Int = 0,
    val accessDeniedSoundDefId: Int = 0,
    val doorOpensForVehicles: Boolean = false,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_DOORPHYSDEF = CLASSID_PHYSICS(0x9000) + 0x80 */
        const val CHUNK_ID: UInt = 0x00020C00u  // PHYSICS_CHUNKID_DOORPHYSDEF

        // Chunk IDs from doors.cpp local enum (line 70)
        private const val CHUNKID_DEF_VARIABLES = 320001903u
        private const val CHUNKID_DEF_PARENT    = 320001904u

        // Micro-chunk IDs from doors.cpp enum (sequential from 2; 1 is obsolete TRIGGER_RADIUS)
        private const val MICRO_CLOSE_DELAY               = 2
        private const val MICRO_TRIGGER_ZONE1             = 3
        private const val MICRO_OLD_LOCK_CODE             = 4
        private const val MICRO_OPEN_SOUND_DEF_ID         = 5
        private const val MICRO_CLOSE_SOUND_DEF_ID        = 6
        private const val MICRO_UNLOCK_SOUND_DEF_ID       = 7
        private const val MICRO_ACCESS_DENIED_SOUND_DEF_ID = 8
        private const val MICRO_TRIGGER_ZONE2             = 9
        private const val MICRO_DOOR_OPENS_FOR_VEHICLES   = 10

        fun load(
            objDataReader: ChunkReader,
            name: String,
            id: UInt,
            chunkId: UInt,
        ): DoorPhysDefClass {
            // Parse AccessiblePhysDefClass parent chain
            val accessibleChunk = objDataReader.findChunk(CHUNKID_DEF_PARENT)

            // StaticAnimPhysDefClass fields (nested inside Accessible -> StaticAnimPhysDef parent)
            val staticAnimChunk = accessibleChunk?.findChunk(AccessiblePhysDefClass.CHUNKID_DEF_PARENT)
            val parentObj = if (staticAnimChunk != null) {
                parseStaticAnimPhysDefClass(staticAnimChunk, name, id, chunkId)
            } else null

            // LockCode from AccessiblePhysDefClass variables
            val parentLockCode = accessibleChunk?.findChunk(AccessiblePhysDefClass.CHUNKID_DEF_VARIABLES)
                ?.let { vars ->
                    val bytes = vars.findMicroChunk(AccessiblePhysDefClass.VARID_DEF_LOCKCODE) ?: return@let null
                    if (bytes.size < 4) return@let null
                    ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
                }

            // Door-specific fields
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
                ?: return DoorPhysDefClass(
                    name = name, id = id, chunkId = chunkId,
                    modelName = parentObj?.modelName ?: "NULL",
                    isPreLit = parentObj?.isPreLit ?: false,
                    isNonOccluder = parentObj?.isNonOccluder ?: true,
                    shadowDynamicObjs = parentObj?.shadowDynamicObjs ?: false,
                    shadowIsAdditive = parentObj?.shadowIsAdditive ?: false,
                    shadowIgnoresZRotation = parentObj?.shadowIgnoresZRotation ?: true,
                    shadowNearZ = parentObj?.shadowNearZ ?: 0.5f,
                    shadowFarZ = parentObj?.shadowFarZ ?: 5.0f,
                    shadowIntensity = parentObj?.shadowIntensity ?: 0.5f,
                    doesCollideInPathfind = parentObj?.doesCollideInPathfind ?: false,
                    isCosmetic = parentObj?.isCosmetic ?: false,
                    animManagerDef = parentObj?.animManagerDef ?: AnimCollisionManagerDef(),
                    projectorManagerDef = parentObj?.projectorManagerDef ?: ProjectorManagerDef(),
                    lockCode = parentLockCode ?: 0,
                )

            val closeDelay           = vars.microChunkFloat(MICRO_CLOSE_DELAY) ?: 2f
            val triggerZone1         = vars.microChunkFloatArray(MICRO_TRIGGER_ZONE1, 15)
            val triggerZone2         = vars.microChunkFloatArray(MICRO_TRIGGER_ZONE2, 15)
            val oldLockCode          = vars.microChunkInt(MICRO_OLD_LOCK_CODE)
            val openSoundDefId       = vars.microChunkInt(MICRO_OPEN_SOUND_DEF_ID) ?: 0
            val closeSoundDefId      = vars.microChunkInt(MICRO_CLOSE_SOUND_DEF_ID) ?: 0
            val unlockSoundDefId     = vars.microChunkInt(MICRO_UNLOCK_SOUND_DEF_ID) ?: 0
            val accessDeniedSoundDefId = vars.microChunkInt(MICRO_ACCESS_DENIED_SOUND_DEF_ID) ?: 0
            val doorOpensForVehicles = vars.microChunkBool(MICRO_DOOR_OPENS_FOR_VEHICLES) ?: false

            // LockCode: prefer parent AccessiblePhysDef value, fall back to legacy OLD_LOCK_CODE
            val lockCode = parentLockCode ?: oldLockCode ?: 0

            return DoorPhysDefClass(
                name = name,
                id = id,
                chunkId = chunkId,
                modelName = parentObj?.modelName ?: "NULL",
                isPreLit = parentObj?.isPreLit ?: false,
                isNonOccluder = parentObj?.isNonOccluder ?: true,
                shadowDynamicObjs = parentObj?.shadowDynamicObjs ?: false,
                shadowIsAdditive = parentObj?.shadowIsAdditive ?: false,
                shadowIgnoresZRotation = parentObj?.shadowIgnoresZRotation ?: true,
                shadowNearZ = parentObj?.shadowNearZ ?: 0.5f,
                shadowFarZ = parentObj?.shadowFarZ ?: 5.0f,
                shadowIntensity = parentObj?.shadowIntensity ?: 0.5f,
                doesCollideInPathfind = parentObj?.doesCollideInPathfind ?: false,
                isCosmetic = parentObj?.isCosmetic ?: false,
                animManagerDef = parentObj?.animManagerDef ?: AnimCollisionManagerDef(),
                projectorManagerDef = parentObj?.projectorManagerDef ?: ProjectorManagerDef(),
                lockCode = lockCode,
                closeDelay = closeDelay,
                triggerZone1 = triggerZone1,
                triggerZone2 = triggerZone2,
                openSoundDefId = openSoundDefId,
                closeSoundDefId = closeSoundDefId,
                unlockSoundDefId = unlockSoundDefId,
                accessDeniedSoundDefId = accessDeniedSoundDefId,
                doorOpensForVehicles = doorOpensForVehicles,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DoorPhysDefClass) return false
        return name == other.name && id == other.id && chunkId == other.chunkId &&
            modelName == other.modelName && isPreLit == other.isPreLit &&
            isNonOccluder == other.isNonOccluder &&
            shadowDynamicObjs == other.shadowDynamicObjs &&
            shadowIsAdditive == other.shadowIsAdditive &&
            shadowIgnoresZRotation == other.shadowIgnoresZRotation &&
            shadowNearZ == other.shadowNearZ && shadowFarZ == other.shadowFarZ &&
            shadowIntensity == other.shadowIntensity &&
            doesCollideInPathfind == other.doesCollideInPathfind &&
            isCosmetic == other.isCosmetic &&
            animManagerDef == other.animManagerDef &&
            projectorManagerDef == other.projectorManagerDef &&
            lockCode == other.lockCode &&
            closeDelay == other.closeDelay &&
            triggerZone1.contentEqualsNullable(other.triggerZone1) &&
            triggerZone2.contentEqualsNullable(other.triggerZone2) &&
            openSoundDefId == other.openSoundDefId &&
            closeSoundDefId == other.closeSoundDefId &&
            unlockSoundDefId == other.unlockSoundDefId &&
            accessDeniedSoundDefId == other.accessDeniedSoundDefId &&
            doorOpensForVehicles == other.doorOpensForVehicles
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + chunkId.hashCode()
        result = 31 * result + modelName.hashCode()
        result = 31 * result + closeDelay.hashCode()
        result = 31 * result + (triggerZone1?.contentHashCode() ?: 0)
        result = 31 * result + (triggerZone2?.contentHashCode() ?: 0)
        result = 31 * result + lockCode
        result = 31 * result + openSoundDefId
        result = 31 * result + closeSoundDefId
        result = 31 * result + unlockSoundDefId
        result = 31 * result + accessDeniedSoundDefId
        result = 31 * result + doorOpensForVehicles.hashCode()
        return result
    }
}

private fun FloatArray?.contentEqualsNullable(other: FloatArray?): Boolean =
    if (this == null) other == null else other != null && contentEquals(other)

// ---------------------------------------------------------------------------
// Micro-chunk reader helpers (private to this file)
// ---------------------------------------------------------------------------

private fun ChunkReader.microChunkInt(id: Int): Int? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
}

private fun ChunkReader.microChunkFloat(id: Int): Float? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.size < 4) return null
    return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float
}

private fun ChunkReader.microChunkBool(id: Int): Boolean? {
    val bytes = findMicroChunk(id) ?: return null
    if (bytes.isEmpty()) return null
    return bytes[0] != 0.toByte()
}

private fun ChunkReader.microChunkFloatArray(id: Int, count: Int): FloatArray? {
    val bytes = findMicroChunk(id) ?: return null
    val needed = count * 4
    if (bytes.size < needed) return null
    val buf = ByteBuffer.wrap(bytes, 0, needed).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(count) { buf.float }
}
