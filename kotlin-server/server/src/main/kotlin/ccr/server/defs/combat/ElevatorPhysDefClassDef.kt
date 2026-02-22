package ccr.server.defs.combat

import ccr.server.defs.AccessiblePhysDefClass
import ccr.server.defs.DefinitionClass
import ccr.server.defs.phys.AnimCollisionManagerDef
import ccr.server.defs.phys.ProjectorManagerDef
import ccr.server.defs.phys.parseStaticAnimPhysDefClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of ElevatorPhysDefClass (Combat/elevator.cpp).
 *
 * C++ hierarchy: ElevatorPhysDefClass : AccessiblePhysDefClass : StaticAnimPhysDefClass :
 *   StaticPhysDefClass : PhysDefClass : DefinitionClass
 *
 * C++ defaults: CloseDelay(2), frame nums(-1), sound IDs(0),
 * zones center(0,0,0) extent(1,1,1).
 *
 * Chunk layout inside OBJDATA:
 *   [714001418] parent (AccessiblePhysDefClass::Save)
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
 *   [714001419] elevator-specific variables
 */
class ElevatorPhysDefClass(
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
    // ElevatorPhysDefClass own fields
    val closeDelay: Float = 2f,
    val lowerCallZone: OBBox = OBBox(),
    val lowerInsideZone: OBBox = OBBox(),
    val upperCallZone: OBBox = OBBox(),
    val upperInsideZone: OBBox = OBBox(),
    val doorClosedTopFrameNum: Float = -1f,
    val doorOpeningBottomFrameNum: Float = -1f,
    val elevatorStartTopFrameNum: Float = -1f,
    val elevatorStoppedBottomFrameNum: Float = -1f,
    val doorOpenSoundDefId: Int = 0,
    val doorCloseSoundDefId: Int = 0,
    val doorUnlockSoundDefId: Int = 0,
    val doorAccessDeniedSoundDefId: Int = 0,
    val elevatorMovingSoundDefId: Int = 0,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        /** CLASSID_ELEVATORPHYSDEF = CLASSID_PHYSICS + 0x81 = 0x9081 */
        const val CHUNK_ID: UInt = 0x00020C01u  // PHYSICS_CHUNKID_ELEVATORPHYSDEF
    }
}

/** Oriented bounding box: Center(3) + Extent(3) + Basis(3x3 rotation matrix). */
data class OBBox(
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val centerZ: Float = 0f,
    val extentX: Float = 1f,
    val extentY: Float = 1f,
    val extentZ: Float = 1f,
    // Basis matrix (3x3, row-major). Default = identity.
    val basis00: Float = 1f, val basis01: Float = 0f, val basis02: Float = 0f,
    val basis10: Float = 0f, val basis11: Float = 1f, val basis12: Float = 0f,
    val basis20: Float = 0f, val basis21: Float = 0f, val basis22: Float = 1f,
)

// Chunk IDs from elevator.cpp local enum (starting at 714001418)
private const val CHUNKID_DEF_PARENT = 714001418u
private const val CHUNKID_DEF_VARIABLES = 714001419u

// Micro-chunk IDs
private const val MCID_CLOSE_DELAY = 1
private const val MCID_UPPER_CALL_ZONE = 3
private const val MCID_UPPER_INSIDE_ZONE = 4
private const val MCID_LOWER_CALL_ZONE = 5
private const val MCID_LOWER_INSIDE_ZONE = 6
private const val MCID_DOORCLOSED_FRAMENUM = 7
private const val MCID_DOOROPENING_FRAMENUM = 8
private const val MCID_ELEVATOR_START_FRAMENUM = 9
private const val MCID_ELEVATOR_STOPPED_FRAMENUM = 10
private const val MCID_DOOR_OPEN_SOUNDID = 11
private const val MCID_DOOR_CLOSE_SOUNDID = 12
private const val MCID_DOOR_UNLOCK_SOUNDID = 13
private const val MCID_DOOR_ACCESS_DENIED_SOUNDID = 14
private const val MCID_ELEVATOR_MOVING_SOUNDID = 15

fun parseElevatorPhysDefClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): ElevatorPhysDefClass? {
    // Parse AccessiblePhysDefClass parent chain
    val accessibleChunk = objDataReader.findChunk(CHUNKID_DEF_PARENT)

    // StaticAnimPhysDefClass fields (nested inside Accessible -> StaticAnimPhysDef parent)
    val staticAnimChunk = accessibleChunk?.findChunk(AccessiblePhysDefClass.CHUNKID_DEF_PARENT)
    val parentObj = if (staticAnimChunk != null) {
        parseStaticAnimPhysDefClass(staticAnimChunk, name, id, chunkId)
    } else null

    // LockCode from AccessiblePhysDefClass variables
    val lockCode = accessibleChunk?.findChunk(AccessiblePhysDefClass.CHUNKID_DEF_VARIABLES)
        ?.let { vars ->
            val bytes = vars.findMicroChunk(AccessiblePhysDefClass.VARID_DEF_LOCKCODE) ?: return@let null
            if (bytes.size < 4) return@let null
            ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        } ?: 0

    // Elevator-specific fields
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES) ?: return null

    var closeDelay = 2f
    var lowerCallZone = OBBox()
    var lowerInsideZone = OBBox()
    var upperCallZone = OBBox()
    var upperInsideZone = OBBox()
    var doorClosedTopFrameNum = -1f
    var doorOpeningBottomFrameNum = -1f
    var elevatorStartTopFrameNum = -1f
    var elevatorStoppedBottomFrameNum = -1f
    var doorOpenSoundDefId = 0
    var doorCloseSoundDefId = 0
    var doorUnlockSoundDefId = 0
    var doorAccessDeniedSoundDefId = 0
    var elevatorMovingSoundDefId = 0

    vars.forEachMicroChunk { mcId, data ->
        when (mcId) {
            MCID_CLOSE_DELAY -> if (data.size >= 4) closeDelay = data.leFloat()
            MCID_UPPER_CALL_ZONE -> upperCallZone = parseOBBox(data)
            MCID_UPPER_INSIDE_ZONE -> upperInsideZone = parseOBBox(data)
            MCID_LOWER_CALL_ZONE -> lowerCallZone = parseOBBox(data)
            MCID_LOWER_INSIDE_ZONE -> lowerInsideZone = parseOBBox(data)
            MCID_DOORCLOSED_FRAMENUM -> if (data.size >= 4) doorClosedTopFrameNum = data.leFloat()
            MCID_DOOROPENING_FRAMENUM -> if (data.size >= 4) doorOpeningBottomFrameNum = data.leFloat()
            MCID_ELEVATOR_START_FRAMENUM -> if (data.size >= 4) elevatorStartTopFrameNum = data.leFloat()
            MCID_ELEVATOR_STOPPED_FRAMENUM -> if (data.size >= 4) elevatorStoppedBottomFrameNum = data.leFloat()
            MCID_DOOR_OPEN_SOUNDID -> if (data.size >= 4) doorOpenSoundDefId = data.leInt()
            MCID_DOOR_CLOSE_SOUNDID -> if (data.size >= 4) doorCloseSoundDefId = data.leInt()
            MCID_DOOR_UNLOCK_SOUNDID -> if (data.size >= 4) doorUnlockSoundDefId = data.leInt()
            MCID_DOOR_ACCESS_DENIED_SOUNDID -> if (data.size >= 4) doorAccessDeniedSoundDefId = data.leInt()
            MCID_ELEVATOR_MOVING_SOUNDID -> if (data.size >= 4) elevatorMovingSoundDefId = data.leInt()
        }
    }

    return ElevatorPhysDefClass(
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
        lowerCallZone = lowerCallZone,
        lowerInsideZone = lowerInsideZone,
        upperCallZone = upperCallZone,
        upperInsideZone = upperInsideZone,
        doorClosedTopFrameNum = doorClosedTopFrameNum,
        doorOpeningBottomFrameNum = doorOpeningBottomFrameNum,
        elevatorStartTopFrameNum = elevatorStartTopFrameNum,
        elevatorStoppedBottomFrameNum = elevatorStoppedBottomFrameNum,
        doorOpenSoundDefId = doorOpenSoundDefId,
        doorCloseSoundDefId = doorCloseSoundDefId,
        doorUnlockSoundDefId = doorUnlockSoundDefId,
        doorAccessDeniedSoundDefId = doorAccessDeniedSoundDefId,
        elevatorMovingSoundDefId = elevatorMovingSoundDefId,
    )
}

/** Parse OBBoxClass: Center(3 floats) + Extent(3 floats) + Basis(3x3 = 9 floats) = 60 bytes. */
private fun parseOBBox(data: ByteArray): OBBox {
    if (data.size < 24) return OBBox()
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val centerX = buf.getFloat(0)
    val centerY = buf.getFloat(4)
    val centerZ = buf.getFloat(8)
    val extentX = buf.getFloat(12)
    val extentY = buf.getFloat(16)
    val extentZ = buf.getFloat(20)
    // Basis matrix: 9 floats starting at offset 24 (total 60 bytes)
    return if (data.size >= 60) {
        OBBox(centerX, centerY, centerZ, extentX, extentY, extentZ,
            buf.getFloat(24), buf.getFloat(28), buf.getFloat(32),
            buf.getFloat(36), buf.getFloat(40), buf.getFloat(44),
            buf.getFloat(48), buf.getFloat(52), buf.getFloat(56))
    } else {
        OBBox(centerX, centerY, centerZ, extentX, extentY, extentZ)
    }
}

private fun ByteArray.leFloat(): Float =
    ByteBuffer.wrap(this, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float

private fun ByteArray.leInt(): Int =
    ByteBuffer.wrap(this, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
