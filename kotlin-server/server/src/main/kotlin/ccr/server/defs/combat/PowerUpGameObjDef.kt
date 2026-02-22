package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroBool
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroInt
import ccr.server.defs.readMicroString
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of PowerUpGameObjDef (Combat/powerup.h).
 *
 * C++ defaults (powerup.cpp constructor):
 *   GrantShieldType(0), GrantShieldStrength(0), GrantShieldStrengthMax(0),
 *   GrantHealth(0), GrantHealthMax(0), GrantWeaponID(0), GrantWeapon(true),
 *   GrantWeaponClips(false), GrantWeaponRounds(0), Persistent(false),
 *   GrantKey(0), GrantSoundID(0), IdleSoundID(0), AlwaysAllowGrant(false)
 */
class PowerUpGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val grantShieldType: Int = 0,
    val grantShieldStrength: Float = 0f,
    val grantShieldStrengthMax: Float = 0f,
    val grantHealth: Float = 0f,
    val grantHealthMax: Float = 0f,
    val grantWeaponId: Int = 0,
    val grantWeapon: Boolean = true,
    val grantWeaponClips: Boolean = false,
    val grantWeaponRounds: Int = 0,
    val persistent: Boolean = false,
    val grantKey: Int = 0,
    val grantSoundId: Int = 0,
    val idleSoundId: Int = 0,
    val grantAnimationName: String = "",
    val idleAnimationName: String = "",
    val alwaysAllowGrant: Boolean = false,
    val physDefId: Int = 0,
) : DefinitionClass(name, id, chunkId) {
    companion object {
        const val CHUNK_ID: UInt = 0x00040107u  // CHUNKID_GAME_OBJECT_DEF_POWERUP
    }
}

// Chunk IDs from powerup.cpp enum
private const val CHUNKID_DEF_VARIABLES = 909991657u // CHUNKID_DEF_PARENT + 1
private const val CHUNKID_DEF_PARENT        = 909991656u  // PowerUpGameObjDef parent wrapper
private const val CHUNKID_SIMPLE_DEF_PARENT = 930991656u  // SimpleGameObjDef parent wrapper
private const val CHUNKID_PHYSICAL_DEF_VARS = 909991657u  // PhysicalGameObjDef CHUNKID_DEF_VARIABLES
private const val MICROCHUNKID_PHYS_ID      = 18          // PhysicalGameObjDef physDefId micro-chunk

// Micro-chunk IDs from powerup.cpp enum
private const val MICROCHUNKID_DEF_PERSISTENT = 2
private const val MICROCHUNKID_DEF_GRANT_SHIELD_TYPE = 3
private const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH = 4
private const val MICROCHUNKID_DEF_GRANT_HEALTH = 6
private const val MICROCHUNKID_DEF_GRANT_WEAPON_ID = 8
private const val MICROCHUNKID_DEF_GRANT_WEAPON = 9
private const val MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS = 10
private const val MICROCHUNKID_DEF_GRANT_ANIMATION_NAME = 13
private const val MICROCHUNKID_DEF_GRANT_SOUNDID = 14
private const val MICROCHUNKID_DEF_IDLE_ANIMATION_NAME = 15
private const val MICROCHUNKID_DEF_IDLE_SOUNDID = 16
private const val MICROCHUNKID_DEF_GRANT_KEY = 17
private const val MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT = 18
private const val MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS = 19
private const val MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX = 20
private const val MICROCHUNKID_DEF_GRANT_HEALTH_MAX = 21

/**
 * Parses a PowerUpGameObjDef from the OBJDATA chunk.
 * [name], [id], and [chunkId] are already extracted by the definition DB reader.
 */
fun parsePowerUpGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): PowerUpGameObjDef {
    val physDefId = objDataReader
        .findChunk(CHUNKID_DEF_PARENT)
        ?.findChunk(CHUNKID_SIMPLE_DEF_PARENT)
        ?.findChunk(CHUNKID_PHYSICAL_DEF_VARS)
        ?.readMicroInt(MICROCHUNKID_PHYS_ID)
        ?: 0

    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return PowerUpGameObjDef(name = name, id = id, chunkId = chunkId, physDefId = physDefId)

    return PowerUpGameObjDef(
        name = name,
        id = id,
        chunkId = chunkId,
        physDefId = physDefId,
        grantShieldType = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SHIELD_TYPE) ?: 0,
        grantShieldStrength = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH) ?: 0f,
        grantShieldStrengthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_SHIELD_STRENGTH_MAX) ?: 0f,
        grantHealth = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH) ?: 0f,
        grantHealthMax = vars.readMicroFloat(MICROCHUNKID_DEF_GRANT_HEALTH_MAX) ?: 0f,
        grantWeaponId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ID) ?: 0,
        grantWeapon = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON) ?: true,
        grantWeaponClips = vars.readMicroBool(MICROCHUNKID_DEF_GRANT_WEAPON_CLIPS) ?: false,
        grantWeaponRounds = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_WEAPON_ROUNDS) ?: 0,
        persistent = vars.readMicroBool(MICROCHUNKID_DEF_PERSISTENT) ?: false,
        grantKey = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_KEY) ?: 0,
        grantSoundId = vars.readMicroInt(MICROCHUNKID_DEF_GRANT_SOUNDID) ?: 0,
        idleSoundId = vars.readMicroInt(MICROCHUNKID_DEF_IDLE_SOUNDID) ?: 0,
        grantAnimationName = vars.readMicroString(MICROCHUNKID_DEF_GRANT_ANIMATION_NAME) ?: "",
        idleAnimationName = vars.readMicroString(MICROCHUNKID_DEF_IDLE_ANIMATION_NAME) ?: "",
        alwaysAllowGrant = vars.readMicroBool(MICROCHUNKID_DEF_ALWAYS_ALLOW_GRANT) ?: false,
    )
}
