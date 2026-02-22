package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

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
        ?: return BeaconGameObjDef(name = name, id = id, chunkId = chunkId)

    return BeaconGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        armingAnimationName = vars.readMicroString(MICROCHUNKID_DEF_ARMING_ANIM_NAME) ?: "",
        broadcastToAllTime = vars.readMicroFloat(MICROCHUNKID_DEF_BROADCAST_TIME) ?: 5f,
        armTime = vars.readMicroFloat(MICROCHUNKID_DEF_ARM_TIME) ?: 10f,
        disarmTime = vars.readMicroFloat(MICROCHUNKID_DEF_DISARM_TIME) ?: 10f,
        preDetonateCinematicDelay = vars.readMicroFloat(MICROCHUNKID_DEF_PRE_DETONATE_CINEMATIC_DELAY) ?: 0f,
        detonateTime = vars.readMicroFloat(MICROCHUNKID_DEF_DETONATE_TIME) ?: 30f,
        postDetonateTime = vars.readMicroFloat(MICROCHUNKID_DEF_POST_DETONATE_TIME) ?: 10f,
        armedSoundDefId = vars.readMicroInt(MICROCHUNKID_DEF_ARMED_SOUNDID) ?: 0,
        disarmingTextId = vars.readMicroInt(MICROCHUNKID_DEF_DISARMING_TEXTID) ?: 0,
        disarmedTextId = vars.readMicroInt(MICROCHUNKID_DEF_DISARMED_TEXTID) ?: 0,
        armingTextId = vars.readMicroInt(MICROCHUNKID_DEF_ARMING_TEXTID) ?: 0,
        armingInterruptedTextId = vars.readMicroInt(MICROCHUNKID_DEF_ARM_INTERRUPT_TEXTID) ?: 0,
        disarmingInterruptedTextId = vars.readMicroInt(MICROCHUNKID_DEF_DISARM_INTERRUPT_TEXTID) ?: 0,
        preDetonateCinematicDefId = vars.readMicroInt(MICROCHUNKID_DEF_PRE_CINEMATIC_DEFID) ?: 0,
        postDetonateCinematicDefId = vars.readMicroInt(MICROCHUNKID_DEF_POST_CINEMATIC_DEFID) ?: 0,
        explosionDefId = vars.readMicroInt(MICROCHUNKID_DEF_EXPLOSION_DEFID) ?: 0,
        isNuke = vars.readMicroInt(MICROCHUNKID_DEF_IS_NUKE) ?: 1,
    )
}
