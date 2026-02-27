package ccr.server.defs

import ccr.server.mix.ChunkReader

// C++: BeaconGameObjDef : public SimpleGameObjDef (beacongameobj.h)
open class BeaconGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,

    // C++: float BroadcastToAllTime (initialized to 5.0f)
    val broadcastToAllTime: Float = 5.0f,

    // C++: float ArmTime (initialized to 10.0f)
    val armTime: Float = 10.0f,

    // C++: float DisarmTime (initialized to 10.0f)
    val disarmTime: Float = 10.0f,

    // C++: float PreDetonateCinematicDelay (initialized to 0)
    val preDetonateCinematicDelay: Float = 0f,

    // C++: float DetonateTime (initialized to 30.0f)
    val detonateTime: Float = 30.0f,

    // C++: float PostDetonateTime (initialized to 10.0f)
    val postDetonateTime: Float = 10.0f,

    // C++: int ArmedSoundDefID (initialized to 0)
    val armedSoundDefId: Int = 0,

    // C++: int DisarmingTextID (initialized to 0)
    val disarmingTextId: Int = 0,

    // C++: int DisarmedTextID (initialized to 0)
    val disarmedTextId: Int = 0,

    // C++: int ArmingTextID (initialized to 0)
    val armingTextId: Int = 0,

    // C++: int ArmingInterruptedTextID (initialized to 0)
    val armingInterruptedTextId: Int = 0,

    // C++: int DisarmingInterruptedTextID (initialized to 0)
    val disarmingInterruptedTextId: Int = 0,

    // C++: int PreDetonateCinematicDefID (initialized to 0)
    val preDetonateCinematicDefId: Int = 0,

    // C++: int PostDetonateCinematicDefID (initialized to 0)
    val postDetonateCinematicDefId: Int = 0,

    // C++: int ExplosionDefID (initialized to 0)
    val explosionDefId: Int = 0,

    // C++: int IsNuke (initialized to true)
    val isNuke: Int = 1,

    // C++: StringClass ArmingAnimationName (initialized to "")
    val armingAnimationName: String = "",
) : SimpleGameObjDef(name, id, chunkId) {

    // C++: bool Is_Nuke() const { return (IsNuke != 0); }
    fun isNuke(): Boolean = isNuke != 0

    companion object {
        // CHUNKID_GAME_OBJECT_DEF_BEACON from combatchunkid.h
        const val CHUNK_ID: UInt = 0x00040136u  // CHUNKID_GAME_OBJECT_DEF_BEACON

        // BeaconGameObjDef save/load chunk IDs (beacongameobj.cpp local enum)
        const val CHUNKID_DEF_PARENT    = 0x02190435
        const val CHUNKID_DEF_VARIABLES = 0x02190436

        // BeaconGameObjDef micro-chunk IDs (beacongameobj.cpp local enum)
        const val MICROCHUNKID_DEF_BROADCAST_TIME               = 1
        const val MICROCHUNKID_DEF_ARM_TIME                     = 2
        const val MICROCHUNKID_DEF_DISARM_TIME                  = 3
        const val MICROCHUNKID_DEF_DETONATE_TIME                = 4
        const val MICROCHUNKID_DEF_ARMED_SOUNDID                = 5
        const val MICROCHUNKID_DEF_DISARMING_TEXTID             = 6
        const val MICROCHUNKID_DEF_DISARMED_TEXTID              = 7
        const val MICROCHUNKID_DEF_ARMING_TEXTID                = 8
        const val MICROCHUNKID_DEF_POST_CINEMATIC_DEFID         = 9
        const val MICROCHUNKID_DEF_ARM_INTERRUPT_TEXTID         = 10
        const val MICROCHUNKID_DEF_DISARM_INTERRUPT_TEXTID      = 11
        const val MICROCHUNKID_DEF_ARMING_ANIM_NAME             = 12
        const val MICROCHUNKID_DEF_PRE_CINEMATIC_DEFID          = 13
        const val MICROCHUNKID_DEF_EXPLOSION_DEFID              = 14
        const val MICROCHUNKID_DEF_POST_DETONATE_TIME           = 15
        const val MICROCHUNKID_DEF_PRE_DETONATE_CINEMATIC_DELAY = 16
        const val MICROCHUNKID_DEF_IS_NUKE                      = 17

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): BeaconGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES.toUInt())
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
    }
}
