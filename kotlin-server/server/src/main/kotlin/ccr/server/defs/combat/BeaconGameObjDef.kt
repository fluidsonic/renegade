package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of BeaconGameObjDef (Combat/beacongameobj.cpp).
 *
 * C++ defaults: BroadcastToAllTime(5), ArmTime(10), DisarmTime(10),
 * PreDetonateCinematicDelay(0), DetonateTime(30), PostDetonateTime(10),
 * ArmedSoundDefID(0), IsNuke(true/1).
 */
class BeaconGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val armingAnimationName: String = "",
    val broadcastToAllTime: Float = 5f,
    val armTime: Float = 10f,
    val disarmTime: Float = 10f,
    val preDetonateCinematicDelay: Float = 0f,
    val detonateTime: Float = 30f,
    val postDetonateTime: Float = 10f,
    val armedSoundDefId: Int = 0,
    val disarmingTextId: Int = 0,
    val disarmedTextId: Int = 0,
    val armingTextId: Int = 0,
    val armingInterruptedTextId: Int = 0,
    val disarmingInterruptedTextId: Int = 0,
    val preDetonateCinematicDefId: Int = 0,
    val postDetonateCinematicDefId: Int = 0,
    val explosionDefId: Int = 0,
    val isNuke: Int = 1,
) : DefinitionClass(name, id, chunkId) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040136u  // CHUNKID_GAME_OBJECT_DEF_BEACON
    }
}

// Chunk IDs from beacongameobj.cpp local enum
private const val CHUNKID_DEF_VARIABLES = 35193910u // 0x02190436

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_BROADCAST_TIME = 1
private const val MICROCHUNKID_DEF_ARM_TIME = 2
private const val MICROCHUNKID_DEF_DISARM_TIME = 3
private const val MICROCHUNKID_DEF_DETONATE_TIME = 4
private const val MICROCHUNKID_DEF_ARMED_SOUNDID = 5
private const val MICROCHUNKID_DEF_DISARMING_TEXTID = 6
private const val MICROCHUNKID_DEF_DISARMED_TEXTID = 7
private const val MICROCHUNKID_DEF_ARMING_TEXTID = 8
private const val MICROCHUNKID_DEF_POST_CINEMATIC_DEFID = 9
private const val MICROCHUNKID_DEF_ARM_INTERRUPT_TEXTID = 10
private const val MICROCHUNKID_DEF_DISARM_INTERRUPT_TEXTID = 11
private const val MICROCHUNKID_DEF_ARMING_ANIM_NAME = 12
private const val MICROCHUNKID_DEF_PRE_CINEMATIC_DEFID = 13
private const val MICROCHUNKID_DEF_EXPLOSION_DEFID = 14
private const val MICROCHUNKID_DEF_POST_DETONATE_TIME = 15
private const val MICROCHUNKID_DEF_PRE_DETONATE_CINEMATIC_DELAY = 16
private const val MICROCHUNKID_DEF_IS_NUKE = 17

fun parseBeaconGameObjDef(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): BeaconGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)

    var armingAnimationName = ""
    var broadcastToAllTime = 5f
    var armTime = 10f
    var disarmTime = 10f
    var preDetonateCinematicDelay = 0f
    var detonateTime = 30f
    var postDetonateTime = 10f
    var armedSoundDefId = 0
    var disarmingTextId = 0
    var disarmedTextId = 0
    var armingTextId = 0
    var armingInterruptedTextId = 0
    var disarmingInterruptedTextId = 0
    var preDetonateCinematicDefId = 0
    var postDetonateCinematicDefId = 0
    var explosionDefId = 0
    var isNuke = 1

    vars?.forEachMicroChunk { microChunkId, bytes ->
        when (microChunkId) {
            MICROCHUNKID_DEF_ARMING_ANIM_NAME -> armingAnimationName = bytes.toNullTerminatedString()
            MICROCHUNKID_DEF_BROADCAST_TIME -> broadcastToAllTime = bytes.toLeFloat()
            MICROCHUNKID_DEF_ARM_TIME -> armTime = bytes.toLeFloat()
            MICROCHUNKID_DEF_DISARM_TIME -> disarmTime = bytes.toLeFloat()
            MICROCHUNKID_DEF_PRE_DETONATE_CINEMATIC_DELAY -> preDetonateCinematicDelay = bytes.toLeFloat()
            MICROCHUNKID_DEF_DETONATE_TIME -> detonateTime = bytes.toLeFloat()
            MICROCHUNKID_DEF_POST_DETONATE_TIME -> postDetonateTime = bytes.toLeFloat()
            MICROCHUNKID_DEF_ARMED_SOUNDID -> armedSoundDefId = bytes.toLeInt()
            MICROCHUNKID_DEF_DISARMING_TEXTID -> disarmingTextId = bytes.toLeInt()
            MICROCHUNKID_DEF_DISARMED_TEXTID -> disarmedTextId = bytes.toLeInt()
            MICROCHUNKID_DEF_ARMING_TEXTID -> armingTextId = bytes.toLeInt()
            MICROCHUNKID_DEF_ARM_INTERRUPT_TEXTID -> armingInterruptedTextId = bytes.toLeInt()
            MICROCHUNKID_DEF_DISARM_INTERRUPT_TEXTID -> disarmingInterruptedTextId = bytes.toLeInt()
            MICROCHUNKID_DEF_PRE_CINEMATIC_DEFID -> preDetonateCinematicDefId = bytes.toLeInt()
            MICROCHUNKID_DEF_POST_CINEMATIC_DEFID -> postDetonateCinematicDefId = bytes.toLeInt()
            MICROCHUNKID_DEF_EXPLOSION_DEFID -> explosionDefId = bytes.toLeInt()
            MICROCHUNKID_DEF_IS_NUKE -> isNuke = bytes.toLeInt()
        }
    }

    return BeaconGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        armingAnimationName = armingAnimationName,
        broadcastToAllTime = broadcastToAllTime,
        armTime = armTime,
        disarmTime = disarmTime,
        preDetonateCinematicDelay = preDetonateCinematicDelay,
        detonateTime = detonateTime,
        postDetonateTime = postDetonateTime,
        armedSoundDefId = armedSoundDefId,
        disarmingTextId = disarmingTextId,
        disarmedTextId = disarmedTextId,
        armingTextId = armingTextId,
        armingInterruptedTextId = armingInterruptedTextId,
        disarmingInterruptedTextId = disarmingInterruptedTextId,
        preDetonateCinematicDefId = preDetonateCinematicDefId,
        postDetonateCinematicDefId = postDetonateCinematicDefId,
        explosionDefId = explosionDefId,
        isNuke = isNuke,
    )
}

private fun ByteArray.toLeInt(): Int =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).int

private fun ByteArray.toLeFloat(): Float =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).float

private fun ByteArray.toNullTerminatedString(): String {
    val nullIndex = indexOfFirst { it == 0.toByte() }
    val len = if (nullIndex < 0) size else nullIndex
    return String(this, 0, len, Charsets.ISO_8859_1)
}
