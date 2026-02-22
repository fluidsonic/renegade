package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of AmmoDefinitionClass (Combat/weaponmanager.h).
 *
 * C++ hierarchy: AmmoDefinitionClass : DefinitionClass
 */
class AmmoDefinitionClass(
    name: String,
    id: UInt,
    chunkId: UInt,
    val modelFilename: String = "",
    val warhead: Int = 0,
    val damage: Float = 1f,
    val range: Float = 10f,
    val effectiveRange: Float = 10f,
    val velocity: Float = 1f,
    val gravity: Float = 0f,
    val elasticity: Float = 1f,
    val rateOfFire: Float = 1f,
    val sprayAngle: Float = 0f,
    val sprayCount: Int = 1,
    val trailEmitter: String = "",
    val aquireTime: Float = 0f,
    val burstDelayTime: Float = 0f,
    val burstMax: Int = 0,
    val softPierceLimit: Int = 0,
    val turnRate: Float = 0f,
    val timeActivated: Boolean = false,
    val terrainActivated: Boolean = false,
    val isTracking: Boolean = false,
    val explosionDefId: Int = 0,
    val randomTrackingScale: Float = 0f,
    val displayLaser: Boolean = false,
    val chargeTime: Float = 0f,
    val continuousSoundDefId: Int = 0,
    val fireSoundDefId: Int = 0,
    val continuousEmitterName: String = "",
    val maxBounces: Int = 0,
    val sprayBulletCost: Int = 1,
    val ammoType: Int = AMMO_TYPE_NORMAL,
    val beaconDefId: Int = 0,
    val c4TriggerTime1: Float = 0f,
    val c4TriggerTime2: Float = 0f,
    val c4TriggerTime3: Float = 0f,
    val c4TriggerRange1: Float = 0f,
    val c4TriggerRange2: Float = 0f,
    val c4TriggerRange3: Float = 0f,
    val c4TimingSound1Id: Int = 0,
    val c4TimingSound2Id: Int = 0,
    val c4TimingSound3Id: Int = 0,
    val aliasedSpeed: Float = 0f,
    val hitterType: Int = 0,
    // Beam effect properties
    val beamEnabled: Boolean = false,
    val beamColorR: Float = 1f,
    val beamColorG: Float = 1f,
    val beamColorB: Float = 1f,
    val beamTime: Float = 0.3f,
    val beamWidth: Float = 0.25f,
    val beamEndCaps: Boolean = true,
    val beamTexture: String = "",
    val beamSubdivisionEnabled: Boolean = false,
    val beamSubdivisionScale: Float = 1f,
    val beamSubdivisionFrozen: Boolean = false,
    // Icon properties
    val iconNameId: Int = 0,
    val iconTextureName: String = "",
    val iconTextureUVLeft: Float = 0f,
    val iconTextureUVTop: Float = 0f,
    val iconTextureUVRight: Float = 0f,
    val iconTextureUVBottom: Float = 0f,
    val iconOffsetX: Float = 0f,
    val iconOffsetY: Float = 0f,
    val grenadeSafetyTime: Float = 0f,
) : DefinitionClass(name, id, chunkId) {

    companion object {
        const val CHUNK_ID: UInt = 0x00040128u  // CHUNKID_AMMO_DEF

        const val AMMO_TYPE_NORMAL = 0
        const val AMMO_TYPE_C4_REMOTE = 1
        const val AMMO_TYPE_C4_TIMED = 2
        const val AMMO_TYPE_C4_PROXIMITY = 3
    }
}

// Chunk IDs from weaponmanager.cpp local enum
private const val CHUNKID_AMMO_DEF_VARIABLES = 1206091429u

// Micro-chunk IDs (sequential enum starting at 1; XXX-prefixed entries are deprecated/unused)
private const val MC_MODEL = 2
private const val MC_WARHEAD = 3
private const val MC_DAMAGE = 4
private const val MC_RANGE = 5
private const val MC_VELOCITY = 6
private const val MC_GRAVITY = 7
private const val MC_SPRAY_ANGLE = 10
private const val MC_SPRAY_COUNT = 11
private const val MC_TRAIL_EMITTER = 12
private const val MC_AQUIRE_TIME = 13
private const val MC_BURST_DELAY_TIME = 14
private const val MC_BURST_MAX = 15
private const val MC_SOFT_PIERCE_LIMIT = 17
private const val MC_TURN_RATE = 18
private const val MC_TIME_ACTIVATED = 19
private const val MC_TERRAIN_ACTIVATED = 20
private const val MC_IS_TRACKING = 21
private const val MC_EFFECTIVE_RANGE = 23
private const val MC_EXPLOSION_DEF_ID = 24
private const val MC_RANDOM_TRACKING_SCALE = 25
private const val MC_DISPLAY_LASER = 26
private const val MC_CHARGE_TIME = 27
private const val MC_CONTINUOUS_SOUND_DEF_ID = 28
private const val MC_CONTINUOUS_EMITTER_NAME = 29
private const val MC_MAX_BOUNCES = 30
private const val MC_SPRAY_BULLET_COST = 31
private const val MC_AMMO_TYPE = 32
private const val MC_C4_TRIGGER_TIME_1 = 35
private const val MC_C4_TRIGGER_TIME_2 = 36
private const val MC_C4_TRIGGER_TIME_3 = 37
private const val MC_C4_TRIGGER_RANGE_1 = 38
private const val MC_C4_TRIGGER_RANGE_2 = 39
private const val MC_C4_TRIGGER_RANGE_3 = 40
private const val MC_C4_TIMING_SOUND_1_ID = 41
private const val MC_C4_TIMING_SOUND_2_ID = 42
private const val MC_C4_TIMING_SOUND_3_ID = 43
private const val MC_ELASTICITY = 44
private const val MC_ALIASED_SPEED = 45
private const val MC_HITTER_TYPE = 46
private const val MC_BEACON_DEFID = 47
private const val MC_RATE_OF_FIRE = 48
private const val MC_BEAM_ENABLED = 49
private const val MC_BEAM_COLOR = 50
private const val MC_BEAM_TIME = 51
private const val MC_BEAM_WIDTH = 52
private const val MC_BEAM_TEXTURE = 53
private const val MC_BEAM_SUBDIVISION_ENABLED = 54
private const val MC_BEAM_SUBDIVISION_SCALE = 55
private const val MC_BEAM_SUBDIVISION_FROZEN = 56
private const val MC_BEAM_END_CAPS = 57
private const val MC_ICON_NAME_ID = 58
private const val MC_ICON_TEXTURE_NAME = 59
private const val MC_ICON_TEXTURE_UV = 60
private const val MC_ICON_OFFSET = 61
private const val MC_FIRE_SOUND_DEFID = 62
private const val MC_GRENADE_SAFETY_TIME = 63

fun parseAmmoDefinitionClass(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    chunkId: UInt,
): AmmoDefinitionClass? {
    val vars = objDataReader.findChunkRecursive(CHUNKID_AMMO_DEF_VARIABLES)
        ?: return null

    return AmmoDefinitionClass(
        name = name,
        id = id,
        chunkId = chunkId,
        modelFilename = vars.readMicroString(MC_MODEL) ?: "",
        warhead = vars.readMicroInt(MC_WARHEAD) ?: 0,
        damage = vars.readMicroFloat(MC_DAMAGE) ?: 1f,
        range = vars.readMicroFloat(MC_RANGE) ?: 10f,
        effectiveRange = vars.readMicroFloat(MC_EFFECTIVE_RANGE) ?: 10f,
        velocity = vars.readMicroFloat(MC_VELOCITY) ?: 1f,
        gravity = vars.readMicroFloat(MC_GRAVITY) ?: 0f,
        elasticity = vars.readMicroFloat(MC_ELASTICITY) ?: 1f,
        rateOfFire = vars.readMicroFloat(MC_RATE_OF_FIRE) ?: 1f,
        sprayAngle = vars.readMicroFloat(MC_SPRAY_ANGLE) ?: 0f,
        sprayCount = vars.readMicroInt(MC_SPRAY_COUNT) ?: 1,
        trailEmitter = vars.readMicroString(MC_TRAIL_EMITTER) ?: "",
        aquireTime = vars.readMicroFloat(MC_AQUIRE_TIME) ?: 0f,
        burstDelayTime = vars.readMicroFloat(MC_BURST_DELAY_TIME) ?: 0f,
        burstMax = vars.readMicroInt(MC_BURST_MAX) ?: 0,
        softPierceLimit = vars.readMicroInt(MC_SOFT_PIERCE_LIMIT) ?: 0,
        turnRate = vars.readMicroFloat(MC_TURN_RATE) ?: 0f,
        timeActivated = vars.readMicroBool(MC_TIME_ACTIVATED) ?: false,
        terrainActivated = vars.readMicroBool(MC_TERRAIN_ACTIVATED) ?: false,
        isTracking = vars.readMicroBool(MC_IS_TRACKING) ?: false,
        explosionDefId = vars.readMicroInt(MC_EXPLOSION_DEF_ID) ?: 0,
        randomTrackingScale = vars.readMicroFloat(MC_RANDOM_TRACKING_SCALE) ?: 0f,
        displayLaser = vars.readMicroBool(MC_DISPLAY_LASER) ?: false,
        chargeTime = vars.readMicroFloat(MC_CHARGE_TIME) ?: 0f,
        continuousSoundDefId = vars.readMicroInt(MC_CONTINUOUS_SOUND_DEF_ID) ?: 0,
        fireSoundDefId = vars.readMicroInt(MC_FIRE_SOUND_DEFID) ?: 0,
        continuousEmitterName = vars.readMicroString(MC_CONTINUOUS_EMITTER_NAME) ?: "",
        maxBounces = vars.readMicroInt(MC_MAX_BOUNCES) ?: 0,
        sprayBulletCost = vars.readMicroInt(MC_SPRAY_BULLET_COST) ?: 1,
        ammoType = vars.readMicroInt(MC_AMMO_TYPE) ?: AmmoDefinitionClass.AMMO_TYPE_NORMAL,
        beaconDefId = vars.readMicroInt(MC_BEACON_DEFID) ?: 0,
        c4TriggerTime1 = vars.readMicroFloat(MC_C4_TRIGGER_TIME_1) ?: 0f,
        c4TriggerTime2 = vars.readMicroFloat(MC_C4_TRIGGER_TIME_2) ?: 0f,
        c4TriggerTime3 = vars.readMicroFloat(MC_C4_TRIGGER_TIME_3) ?: 0f,
        c4TriggerRange1 = vars.readMicroFloat(MC_C4_TRIGGER_RANGE_1) ?: 0f,
        c4TriggerRange2 = vars.readMicroFloat(MC_C4_TRIGGER_RANGE_2) ?: 0f,
        c4TriggerRange3 = vars.readMicroFloat(MC_C4_TRIGGER_RANGE_3) ?: 0f,
        c4TimingSound1Id = vars.readMicroInt(MC_C4_TIMING_SOUND_1_ID) ?: 0,
        c4TimingSound2Id = vars.readMicroInt(MC_C4_TIMING_SOUND_2_ID) ?: 0,
        c4TimingSound3Id = vars.readMicroInt(MC_C4_TIMING_SOUND_3_ID) ?: 0,
        aliasedSpeed = vars.readMicroFloat(MC_ALIASED_SPEED) ?: 0f,
        hitterType = vars.readMicroInt(MC_HITTER_TYPE) ?: 0,
        beamEnabled = vars.readMicroBool(MC_BEAM_ENABLED) ?: false,
        beamColorR = vars.readMicroFloatAt(MC_BEAM_COLOR, 0) ?: 1f,
        beamColorG = vars.readMicroFloatAt(MC_BEAM_COLOR, 4) ?: 1f,
        beamColorB = vars.readMicroFloatAt(MC_BEAM_COLOR, 8) ?: 1f,
        beamTime = vars.readMicroFloat(MC_BEAM_TIME) ?: 0.3f,
        beamWidth = vars.readMicroFloat(MC_BEAM_WIDTH) ?: 0.25f,
        beamEndCaps = vars.readMicroBool(MC_BEAM_END_CAPS) ?: true,
        beamTexture = vars.readMicroString(MC_BEAM_TEXTURE) ?: "",
        beamSubdivisionEnabled = vars.readMicroBool(MC_BEAM_SUBDIVISION_ENABLED) ?: false,
        beamSubdivisionScale = vars.readMicroFloat(MC_BEAM_SUBDIVISION_SCALE) ?: 1f,
        beamSubdivisionFrozen = vars.readMicroBool(MC_BEAM_SUBDIVISION_FROZEN) ?: false,
        iconNameId = vars.readMicroInt(MC_ICON_NAME_ID) ?: 0,
        iconTextureName = vars.readMicroString(MC_ICON_TEXTURE_NAME) ?: "",
        iconTextureUVLeft = vars.readMicroFloatAt(MC_ICON_TEXTURE_UV, 0) ?: 0f,
        iconTextureUVTop = vars.readMicroFloatAt(MC_ICON_TEXTURE_UV, 4) ?: 0f,
        iconTextureUVRight = vars.readMicroFloatAt(MC_ICON_TEXTURE_UV, 8) ?: 0f,
        iconTextureUVBottom = vars.readMicroFloatAt(MC_ICON_TEXTURE_UV, 12) ?: 0f,
        iconOffsetX = vars.readMicroFloatAt(MC_ICON_OFFSET, 0) ?: 0f,
        iconOffsetY = vars.readMicroFloatAt(MC_ICON_OFFSET, 4) ?: 0f,
        grenadeSafetyTime = vars.readMicroFloat(MC_GRENADE_SAFETY_TIME) ?: 0f,
    )
}
