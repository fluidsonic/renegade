package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** DEG_TO_RADF(360) from C++ = 2 * PI */
private const val DEG_TO_RAD_360 = 6.2831855f // Float.fromBits(0x40C90FDB) — matches C++ DEG_TO_RADF(360.0f)

// ---------------------------------------------------------------------------
// Helpers to extract typed values from micro-chunk byte arrays
// ---------------------------------------------------------------------------

internal fun ByteArray.toLeInt(): Int =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).int

internal fun ByteArray.toLeFloat(): Float =
    ByteBuffer.wrap(this, 0, 4.coerceAtMost(size)).order(ByteOrder.LITTLE_ENDIAN).float

internal fun ByteArray.toBool(): Boolean = isNotEmpty() && this[0] != 0.toByte()

internal fun ByteArray.toNullTerminatedString(): String {
    val nullIndex = indexOfFirst { it == 0.toByte() }
    val len = if (nullIndex < 0) size else nullIndex
    return String(this, 0, len, Charsets.ISO_8859_1)
}


// ---------------------------------------------------------------------------
// DefinitionClass base (id + name)
// Chunk structure: CHUNKID_VARIABLES(0x100) { micro[0x01]=id, micro[0x03]=name }
// ---------------------------------------------------------------------------
data class DefinitionData(
    val id: UInt,
    val name: String,
) {
    companion object {
        private const val CHUNKID_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID = 0x01
        private const val VARID_NAME = 0x03

        fun load(reader: ChunkReader): DefinitionData? {
            val vars = reader.findChunk(CHUNKID_VARIABLES) ?: return null
            val id = vars.readMicroInt(VARID_INSTANCEID)?.toUInt() ?: return null
            val name = vars.readMicroString(VARID_NAME) ?: return null
            return DefinitionData(id = id, name = name)
        }
    }
}

// ---------------------------------------------------------------------------
// BaseGameObjDef — wraps DefinitionClass in chunk 1111991123
// ---------------------------------------------------------------------------
// No own fields, just delegates.

internal const val BASEGAMEOBJ_CHUNKID_DEF_PARENT = 1111991123u

// ---------------------------------------------------------------------------
// ScriptableGameObjDef
// parent chunk = 627001056, variables chunk = 627001057
// micro[2] = script name (string), micro[3] = script parameters (string)
// Scripts come in paired name+params micro-chunks inside the variables chunk.
// ---------------------------------------------------------------------------
data class ScriptEntry(val name: String, val parameters: String)

data class ScriptableGameObjDefData(
    val scripts: List<ScriptEntry>,
) {
    companion object {
        private const val CHUNKID_DEF_PARENT = 627001056u
        private const val CHUNKID_DEF_VARIABLES = 627001057u
        private const val MICROCHUNKID_DEF_SCRIPT_NAME = 2
        private const val MICROCHUNKID_DEF_SCRIPT_PARAMETERS = 3

        fun load(reader: ChunkReader): Pair<ScriptableGameObjDefData, ChunkReader?>? {
            var parentChunk: ChunkReader? = null
            var variablesChunk: ChunkReader? = null
            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> parentChunk = child
                    CHUNKID_DEF_VARIABLES -> variablesChunk = child
                }
            }

            val scripts = mutableListOf<ScriptEntry>()
            variablesChunk?.let { vars ->
                val names = mutableListOf<String>()
                val params = mutableListOf<String>()
                vars.forEachMicroChunk { microId, microData ->
                    when (microId) {
                        MICROCHUNKID_DEF_SCRIPT_NAME -> names += microData.toNullTerminatedString()
                        MICROCHUNKID_DEF_SCRIPT_PARAMETERS -> params += microData.toNullTerminatedString()
                    }
                }
                for (i in 0 until minOf(names.size, params.size)) {
                    scripts += ScriptEntry(names[i], params[i])
                }
            }

            return ScriptableGameObjDefData(scripts) to parentChunk
        }
    }
}

// ---------------------------------------------------------------------------
// DefenseObjectDefClass
// Chunk: DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607
// micro[0x00]=health, [0x01]=healthMax, [0x02]=skin(save_id), [0x03]=shieldStrength,
// [0x04]=shieldStrengthMax, [0x05]=shieldType(save_id), [0x06]=damagePoints, [0x07]=deathPoints
// ---------------------------------------------------------------------------
data class DefenseObjectDef(
    val health: Float = 100f,
    val healthMax: Float = 100f,
    val skinSaveId: Int = 0,
    val shieldStrength: Float = 0f,
    val shieldStrengthMax: Float = 0f,
    val shieldTypeSaveId: Int = 0,
    val damagePoints: Float = 0f,
    val deathPoints: Float = 0f,
) {
    fun toDefenseObjectDefClass() = DefenseObjectDefClass(
        health = health,
        healthMax = healthMax,
        skin = skinSaveId,
        shieldStrength = shieldStrength,
        shieldStrengthMax = shieldStrengthMax,
        shieldType = shieldTypeSaveId,
        damagePoints = damagePoints,
        deathPoints = deathPoints,
    )

    companion object {
        private const val DEFENSEOBJECTDEF_CHUNK_VARIABLES = 7311607u
        private const val VARIABLE_HEALTH = 0x00
        private const val VARIABLE_HEALTHMAX = 0x01
        private const val VARIABLE_SKIN = 0x02
        private const val VARIABLE_SHIELDSTRENGTH = 0x03
        private const val VARIABLE_SHIELDSTRENGTHMAX = 0x04
        private const val VARIABLE_SHIELDTYPE = 0x05
        private const val VARIABLE_DAMAGE_POINTS = 0x06
        private const val VARIABLE_DEATH_POINTS = 0x07

        fun load(reader: ChunkReader): DefenseObjectDef {
            var def = DefenseObjectDef()
            reader.forEachChunk { id, _, child ->
                if (id == DEFENSEOBJECTDEF_CHUNK_VARIABLES) {
                    child.forEachMicroChunk { microId, microData ->
                        when (microId) {
                            VARIABLE_HEALTH -> def = def.copy(health = microData.toLeFloat())
                            VARIABLE_HEALTHMAX -> def = def.copy(healthMax = microData.toLeFloat())
                            VARIABLE_SKIN -> def = def.copy(skinSaveId = microData.toLeInt())
                            VARIABLE_SHIELDSTRENGTH -> def = def.copy(shieldStrength = microData.toLeFloat())
                            VARIABLE_SHIELDSTRENGTHMAX -> def = def.copy(shieldStrengthMax = microData.toLeFloat())
                            VARIABLE_SHIELDTYPE -> def = def.copy(shieldTypeSaveId = microData.toLeInt())
                            VARIABLE_DAMAGE_POINTS -> def = def.copy(damagePoints = microData.toLeFloat())
                            VARIABLE_DEATH_POINTS -> def = def.copy(deathPoints = microData.toLeFloat())
                        }
                    }
                }
            }
            return def
        }
    }
}

// ---------------------------------------------------------------------------
// DamageableGameObjDef
// parent = 207011205, variables = 207011206, defenseObjectDef = 207011207
// micro[1]=translatedNameID, [2]=infoIconTexture(string), [3]=encyclopediaType,
// [4]=encyclopediaID, [5]=notTargetable, [6]=defaultPlayerType
// ---------------------------------------------------------------------------
data class DamageableGameObjDefData(
    val defenseObjectDef: DefenseObjectDef = DefenseObjectDef(),
    val translatedNameId: Int = 0,
    val infoIconTextureFilename: String = "",
    val encyclopediaType: Int = 0,
    val encyclopediaId: Int = 0,
    val notTargetable: Boolean = false,
    val defaultPlayerType: Int = -2, // PLAYERTYPE_NEUTRAL
) {
    companion object {
        private const val CHUNKID_DEF_PARENT = 207011205u
        private const val CHUNKID_DEF_VARIABLES = 207011206u
        private const val CHUNKID_DEF_DEFENSEOBJECTDEF = 207011207u
        private const val MICROCHUNKID_DEF_TRANSLATED_NAME_ID = 1
        private const val MICROCHUNKID_DEF_INFO_ICON_TEXTURE_FILENAME = 2
        private const val MICROCHUNKID_DEF_ENCY_TYPE = 3
        private const val MICROCHUNKID_DEF_ENCY_ID = 4
        private const val MICROCHUNKID_DEF_NOT_TARGETABLE = 5
        private const val MICROCHUNKID_DEF_DEFAULT_PLAYER_TYPE = 6

        fun load(reader: ChunkReader): Pair<DamageableGameObjDefData, ChunkReader?>? {
            var parentChunk: ChunkReader? = null
            var data = DamageableGameObjDefData()

            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> parentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_DEF_TRANSLATED_NAME_ID -> data = data.copy(translatedNameId = microData.toLeInt())
                                MICROCHUNKID_DEF_INFO_ICON_TEXTURE_FILENAME -> data = data.copy(infoIconTextureFilename = microData.toNullTerminatedString())
                                MICROCHUNKID_DEF_ENCY_TYPE -> data = data.copy(encyclopediaType = microData.toLeInt())
                                MICROCHUNKID_DEF_ENCY_ID -> data = data.copy(encyclopediaId = microData.toLeInt())
                                MICROCHUNKID_DEF_NOT_TARGETABLE -> data = data.copy(notTargetable = microData.toBool())
                                MICROCHUNKID_DEF_DEFAULT_PLAYER_TYPE -> data = data.copy(defaultPlayerType = microData.toLeInt())
                            }
                        }
                    }
                    CHUNKID_DEF_DEFENSEOBJECTDEF -> data = data.copy(defenseObjectDef = DefenseObjectDef.load(child))
                }
            }

            return data to parentChunk
        }
    }
}

// ---------------------------------------------------------------------------
// PhysicalGameObjDef
// parent = 909991661 (CHUNKID_DEF_PARENT), variables = 909991657
// micro[1]=type, [2]=bullseyeOffsetZ, [4]=radarBlipType, [17]=animation(string),
// [18]=physDefID, [19]=legacy_defaultPlayerType, [20]=killedExplosion,
// [21]=legacy_translatedNameID, [22]=defaultHibernationEnable,
// [23]=allowInnateConversations, [24]=oratorType, [25]=useCreationEffect
// ---------------------------------------------------------------------------
data class PhysicalGameObjDefData(
    val type: Int = 0,
    val bullseyeOffsetZ: Float = 0f,
    val radarBlipType: Int = 0,
    val animation: String = "",
    val physDefId: Int = 0,
    val killedExplosion: Int = 0,
    val defaultHibernationEnable: Boolean = true,
    val allowInnateConversations: Boolean = false,
    val oratorType: Int = 999, // ORATOR_TYPE_START(1000) - 1
    val useCreationEffect: Boolean = false,
) {
    companion object {
        // PhysicalGameObjDef chunk IDs — enum starts at 909991657
        private const val CHUNKID_DEF_VARIABLES = 909991657u
        // LEGACY_CHUNKID_DEF_PARENT_OLD = 909991658u (old parent wrapping ScriptableGameObjDef)
        // XXXCHUNKID_DEF_PARENT_OLD_OLD = 909991659u
        // LEGACY_CHUNKID_DEF_DEFENSEOBJECTDEF = 909991660u
        private const val CHUNKID_DEF_PARENT = 909991661u

        private const val MICROCHUNKID_DEF_TYPE = 1
        private const val MICROCHUNKID_DEF_BULLSEYE_OFFSET_Z = 2
        private const val MICROCHUNKID_DEF_BLIP_TYPE = 4
        private const val MICROCHUNKID_DEF_ANIMATION = 17
        private const val MICROCHUNKID_DEF_PHYS_ID = 18
        private const val MICROCHUNKID_DEF_KILLED_EXPLOSION = 20
        private const val MICROCHUNKID_DEF_DEFAULT_HIBERNATION_ENABLE = 22
        private const val MICROCHUNKID_DEF_ALLOW_INNATE_CONVERSATIONS = 23
        private const val MICROCHUNKID_DEF_ORATOR_TYPE = 24
        private const val MICROCHUNKID_DEF_USE_CREATION_EFFECT = 25

        fun load(reader: ChunkReader): Pair<PhysicalGameObjDefData, ChunkReader?>? {
            var parentChunk: ChunkReader? = null
            var data = PhysicalGameObjDefData()

            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> parentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_DEF_TYPE -> data = data.copy(type = microData.toLeInt())
                                MICROCHUNKID_DEF_BULLSEYE_OFFSET_Z -> data = data.copy(bullseyeOffsetZ = microData.toLeFloat())
                                MICROCHUNKID_DEF_BLIP_TYPE -> data = data.copy(radarBlipType = microData.toLeInt())
                                MICROCHUNKID_DEF_ANIMATION -> data = data.copy(animation = microData.toNullTerminatedString())
                                MICROCHUNKID_DEF_PHYS_ID -> data = data.copy(physDefId = microData.toLeInt())
                                MICROCHUNKID_DEF_KILLED_EXPLOSION -> data = data.copy(killedExplosion = microData.toLeInt())
                                MICROCHUNKID_DEF_DEFAULT_HIBERNATION_ENABLE -> data = data.copy(defaultHibernationEnable = microData.toBool())
                                MICROCHUNKID_DEF_ALLOW_INNATE_CONVERSATIONS -> data = data.copy(allowInnateConversations = microData.toBool())
                                MICROCHUNKID_DEF_ORATOR_TYPE -> data = data.copy(oratorType = microData.toLeInt())
                                MICROCHUNKID_DEF_USE_CREATION_EFFECT -> data = data.copy(useCreationEffect = microData.toBool())
                            }
                        }
                    }
                }
            }

            return data to parentChunk
        }
    }
}

// ---------------------------------------------------------------------------
// ArmedGameObjDef
// parent = 418001829, variables = 418001830
// micro[1]=weaponTiltRate, [2]=tiltMin, [3]=tiltMax, [4]=turnRate, [5]=turnMin,
// [6]=turnMax, [11]=weaponDefID, [12]=weaponRounds, [13]=weaponError,
// [14]=secondaryWeaponDefID
// ---------------------------------------------------------------------------
data class ArmedGameObjDefData(
    val weaponTiltRate: Float = 1f,
    val weaponTiltMin: Float = -10000f,
    val weaponTiltMax: Float = 10000f,
    val weaponTurnRate: Float = 1f,
    val weaponTurnMin: Float = -10000f,
    val weaponTurnMax: Float = 10000f,
    val weaponError: Float = 0f,
    val weaponDefId: Int = 0,
    val secondaryWeaponDefId: Int = 0,
    val weaponRounds: Int = -1,
) {
    companion object {
        private const val CHUNKID_DEF_PARENT = 418001829u
        private const val CHUNKID_DEF_VARIABLES = 418001830u

        private const val MICROCHUNKID_DEF_WEAPON_TILT_RATE = 1
        private const val MICROCHUNKID_DEF_WEAPON_TILT_MIN = 2
        private const val MICROCHUNKID_DEF_WEAPON_TILT_MAX = 3
        private const val MICROCHUNKID_DEF_WEAPON_TURN_RATE = 4
        private const val MICROCHUNKID_DEF_WEAPON_TURN_MIN = 5
        private const val MICROCHUNKID_DEF_WEAPON_TURN_MAX = 6
        private const val MICROCHUNKID_DEF_WEAPON_DEF_ID = 11
        private const val MICROCHUNKID_DEF_WEAPON_ROUNDS = 12
        private const val MICROCHUNKID_DEF_WEAPON_ERROR = 13
        private const val MICROCHUNKID_DEF_SECONDARY_WEAPON_DEF_ID = 14

        fun load(reader: ChunkReader): Pair<ArmedGameObjDefData, ChunkReader?>? {
            var parentChunk: ChunkReader? = null
            var data = ArmedGameObjDefData()

            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> parentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_DEF_WEAPON_TILT_RATE -> data = data.copy(weaponTiltRate = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_TILT_MIN -> data = data.copy(weaponTiltMin = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_TILT_MAX -> data = data.copy(weaponTiltMax = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_TURN_RATE -> data = data.copy(weaponTurnRate = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_TURN_MIN -> data = data.copy(weaponTurnMin = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_TURN_MAX -> data = data.copy(weaponTurnMax = microData.toLeFloat())
                                MICROCHUNKID_DEF_WEAPON_DEF_ID -> data = data.copy(weaponDefId = microData.toLeInt())
                                MICROCHUNKID_DEF_SECONDARY_WEAPON_DEF_ID -> data = data.copy(secondaryWeaponDefId = microData.toLeInt())
                                MICROCHUNKID_DEF_WEAPON_ROUNDS -> data = data.copy(weaponRounds = microData.toLeInt())
                                MICROCHUNKID_DEF_WEAPON_ERROR -> data = data.copy(weaponError = microData.toLeFloat())
                            }
                        }
                    }
                }
            }

            return data to parentChunk
        }
    }
}

// ---------------------------------------------------------------------------
// SmartGameObjDef
// armed-parent = 909991658 (CHUNKID_DEF_ARMEDGAMEOBJ_PARENT),
// variables = 909991657 (CHUNKID_DEF_VARIABLES)
// micro[9]=sightRange, [10]=sightArc, [17]=listenerScale, [19]=isStealthUnit
// ---------------------------------------------------------------------------
data class SmartGameObjDefData(
    val sightRange: Float = 0f,
    val sightArc: Float = 0f,
    val listenerScale: Float = 1f,
    val isStealthUnit: Boolean = false,
) {
    companion object {
        // The enum reuses 909991656 as XXX (obsolete), then +1=variables, +2=armed parent
        private const val CHUNKID_DEF_VARIABLES = 909991657u
        private const val CHUNKID_DEF_ARMEDGAMEOBJ_PARENT = 909991658u

        private const val MICROCHUNKID_DEF_SIGHT_RANGE = 9
        private const val MICROCHUNKID_DEF_SIGHT_ARC = 10
        private const val MICROCHUNKID_DEF_LISTENER_SCALE = 17
        private const val MICROCHUNKID_DEF_IS_STEALTH_UNIT = 19

        fun load(reader: ChunkReader): Pair<SmartGameObjDefData, ChunkReader?>? {
            var armedParentChunk: ChunkReader? = null
            var data = SmartGameObjDefData()

            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_ARMEDGAMEOBJ_PARENT -> armedParentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_DEF_SIGHT_RANGE -> data = data.copy(sightRange = microData.toLeFloat())
                                MICROCHUNKID_DEF_SIGHT_ARC -> data = data.copy(sightArc = microData.toLeFloat())
                                MICROCHUNKID_DEF_LISTENER_SCALE -> data = data.copy(listenerScale = microData.toLeFloat())
                                MICROCHUNKID_DEF_IS_STEALTH_UNIT -> data = data.copy(isStealthUnit = microData.toBool())
                            }
                        }
                    }
                }
            }

            return data to armedParentChunk
        }
    }
}

// ---------------------------------------------------------------------------
// DialogueOptionClass
// CHUNKID_OPTION_VARIABLES = 0x08040528
// micro[0]=weight(float), [2]=conversationID(int)
// ---------------------------------------------------------------------------
data class DialogueOption(
    val weight: Float = 0f,
    val conversationId: Int = 0,
) {
    companion object {
        private const val CHUNKID_OPTION_VARIABLES = 0x08040528u

        private const val VARID_WEIGHT = 0
        private const val VARID_CONVERSATION_ID = 2

        fun load(reader: ChunkReader): DialogueOption {
            var option = DialogueOption()
            reader.forEachChunk { id, _, child ->
                if (id == CHUNKID_OPTION_VARIABLES) {
                    child.forEachMicroChunk { microId, microData ->
                        when (microId) {
                            VARID_WEIGHT -> option = option.copy(weight = microData.toLeFloat())
                            VARID_CONVERSATION_ID -> option = option.copy(conversationId = microData.toLeInt())
                        }
                    }
                }
            }
            return option
        }
    }
}

// ---------------------------------------------------------------------------
// DialogueClass
// CHUNKID_DIALOGUE_VARIABLES = 0x08040529, CHUNKID_DIALOGUE_OPTION = 0x0804052A
// micro[0]=silenceWeight(float)
// ---------------------------------------------------------------------------
data class Dialogue(
    val silenceWeight: Float = 0f,
    val options: List<DialogueOption> = emptyList(),
) {
    companion object {
        private const val CHUNKID_DIALOGUE_VARIABLES = 0x08040529u
        private const val CHUNKID_DIALOGUE_OPTION = 0x0804052Au

        private const val VARID_DIALOGUE_SILENCE = 0

        fun load(reader: ChunkReader): Dialogue {
            var silenceWeight = 0f
            val options = mutableListOf<DialogueOption>()
            reader.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DIALOGUE_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            if (microId == VARID_DIALOGUE_SILENCE) {
                                silenceWeight = microData.toLeFloat()
                            }
                        }
                    }
                    CHUNKID_DIALOGUE_OPTION -> options += DialogueOption.load(child)
                }
            }
            return Dialogue(silenceWeight = silenceWeight, options = options)
        }
    }
}

// ---------------------------------------------------------------------------
// SoldierGameObjDef — top-level definition for soldier presets
//
// C++ hierarchy: SoldierGameObjDef : SmartGameObjDef : ArmedGameObjDef :
//   PhysicalGameObjDef : DamageableGameObjDef : ScriptableGameObjDef :
//   BaseGameObjDef : DefinitionClass
//
// Chunk IDs (SoldierGameObjDef local enum starting at 909991656):
//   CHUNKID_DEF_PARENT       = 909991656
//   CHUNKID_DEF_VARIABLES    = 909991657
//   CHUNKID_DEF_DIALOG_ENTRY = 909991658
//
// Micro-chunk IDs inside CHUNKID_DEF_VARIABLES:
//   1=turnRate, 2=jumpVelocity, 3=skeletonHeight, 4=skeletonWidth,
//   5=useInnateBehavior, 6=innateAggressiveness, 7=innateTakeCoverProb,
//   10=firstPersonHands(string), 13=innateIsStationary,
//   14=oratorType, 15=humanAnimOverrideDefID, 16=deathSoundPresetID,
//   17=humanLoiterCollectionDefID
// ---------------------------------------------------------------------------
open class SoldierGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,

    // ScriptableGameObjDef
    val scriptable: ScriptableGameObjDefData,

    // DamageableGameObjDef
    val damageable: DamageableGameObjDefData,

    // PhysicalGameObjDef
    val physical: PhysicalGameObjDefData,

    // ArmedGameObjDef
    val armed: ArmedGameObjDefData,

    // SmartGameObjDef
    val smart: SmartGameObjDefData,

    // SoldierGameObjDef own fields
    val turnRate: Float = DEG_TO_RAD_360,
    val jumpVelocity: Float = 2f,
    val skeletonHeight: Float = 0f,
    val skeletonWidth: Float = 0f,
    val useInnateBehavior: Boolean = true,
    val innateAggressiveness: Float = 0.5f,
    val innateTakeCoverProbability: Float = 0.5f,
    val innateIsStationary: Boolean = false,
    val firstPersonHands: String = "",
    val humanAnimOverrideDefId: Int = 0,
    val humanLoiterCollectionDefId: Int = 0,
    val deathSoundPresetId: Int = 0,
    val dialogList: List<Dialogue> = emptyList(),
) : SmartGameObjDef(
    name, id, chunkId,
    // SmartGameObjDef own fields
    sightRange = smart.sightRange,
    sightArc = smart.sightArc,
    listenerScale = smart.listenerScale,
    isStealthUnit = smart.isStealthUnit,
    // ArmedGameObjDef fields
    weaponDefId = armed.weaponDefId,
    secondaryWeaponDefId = armed.secondaryWeaponDefId,
    weaponRounds = armed.weaponRounds,
    weaponTiltRate = armed.weaponTiltRate,
    weaponTiltMin = armed.weaponTiltMin,
    weaponTiltMax = armed.weaponTiltMax,
    weaponTurnRate = armed.weaponTurnRate,
    weaponTurnMin = armed.weaponTurnMin,
    weaponTurnMax = armed.weaponTurnMax,
    weaponError = armed.weaponError,
    // PhysicalGameObjDef fields
    type = physical.type,
    radarBlipType = physical.radarBlipType,
    bullseyeOffsetZ = physical.bullseyeOffsetZ,
    animation = physical.animation,
    physDefId = physical.physDefId,
    killedExplosion = physical.killedExplosion,
    defaultHibernationEnable = physical.defaultHibernationEnable,
    allowInnateConversations = physical.allowInnateConversations,
    oratorType = physical.oratorType,
    useCreationEffect = physical.useCreationEffect,
    // DamageableGameObjDef fields
    defenseObjectDef = damageable.defenseObjectDef.toDefenseObjectDefClass(),
    infoIconTextureFilename = damageable.infoIconTextureFilename,
    translatedNameId = damageable.translatedNameId,
    notTargetable = damageable.notTargetable,
    defaultPlayerType = damageable.defaultPlayerType,
    encyclopediaType = damageable.encyclopediaType,
    encyclopediaId = damageable.encyclopediaId,
    // ScriptableGameObjDef fields
    scriptNameList = scriptable.scripts.map { it.name },
    scriptParameterList = scriptable.scripts.map { it.parameters },
) {

    companion object {
        const val CHUNK_ID: UInt = 0x0004010Fu  // CHUNKID_GAME_OBJECT_DEF_SOLDIER

        // SoldierGameObjDef chunk IDs
        private const val CHUNKID_DEF_PARENT = 909991656u
        private const val CHUNKID_DEF_VARIABLES = 909991657u
        private const val CHUNKID_DEF_DIALOG_ENTRY = 909991658u

        // SoldierGameObjDef micro-chunk IDs
        private const val MICROCHUNKID_DEF_TURN_RATE = 1
        private const val MICROCHUNKID_DEF_JUMP_VELOCITY = 2
        private const val MICROCHUNKID_DEF_SKELETON_HEIGHT = 3
        private const val MICROCHUNKID_DEF_SKELETON_WIDTH = 4
        private const val MICROCHUNKID_DEF_USE_INNATE_BEHAVIOR = 5
        private const val MICROCHUNKID_DEF_INNATE_AGGRESSIVENESS = 6
        private const val MICROCHUNKID_DEF_INNATE_TAKE_COVER_PROB = 7
        private const val MICROCHUNKID_DEF_FIRST_PERSON_HANDS = 10
        private const val MICROCHUNKID_DEF_INNATE_IS_STATIONARY = 13
        private const val MICROCHUNKID_DEF_ORATOR_TYPE = 14
        private const val MICROCHUNKID_DEF_HUMAN_ANIM_OVERRIDE_DEF_ID = 15
        private const val MICROCHUNKID_DEF_DEATH_SOUND_PRESET = 16
        private const val MICROCHUNKID_DEF_HUMAN_LOITER_COLLECTION_DEF_ID = 17

        private const val DIALOG_MAX = 20

        /**
         * Loads a SoldierGameObjDef from the OBJDATA chunk for a soldier definition.
         * [objDataChunk] should be the ChunkReader for the SIMPLEFACTORY_CHUNKID_OBJDATA content.
         */
        fun load(objDataChunk: ChunkReader, name: String, id: UInt, chunkId: UInt): SoldierGameObjDef? =
            parseFields(objDataChunk)?.let { fields ->
                SoldierGameObjDef(
                    name = name, id = id, chunkId = chunkId,
                    scriptable = fields.scriptable,
                    damageable = fields.damageable,
                    physical = fields.physical,
                    armed = fields.armed,
                    smart = fields.smart,
                    turnRate = fields.turnRate,
                    jumpVelocity = fields.jumpVelocity,
                    skeletonHeight = fields.skeletonHeight,
                    skeletonWidth = fields.skeletonWidth,
                    useInnateBehavior = fields.useInnateBehavior,
                    innateAggressiveness = fields.innateAggressiveness,
                    innateTakeCoverProbability = fields.innateTakeCoverProbability,
                    innateIsStationary = fields.innateIsStationary,
                    firstPersonHands = fields.firstPersonHands,
                    humanAnimOverrideDefId = fields.humanAnimOverrideDefId,
                    humanLoiterCollectionDefId = fields.humanLoiterCollectionDefId,
                    deathSoundPresetId = fields.deathSoundPresetId,
                    dialogList = fields.dialogList,
                )
            }

        /**
         * Parses all SoldierGameObjDef fields from an OBJDATA chunk without constructing
         * the final object. Used by subclass loaders (MendozaBoss, RaveshawBoss) that need
         * to extract the fields and call their own constructors.
         */
        internal fun parseFields(objDataChunk: ChunkReader): ParsedSoldierFields? {
            // --- SoldierGameObjDef layer ---
            var smartParentChunk: ChunkReader? = null
            var turnRate = DEG_TO_RAD_360
            var jumpVelocity = 2f
            var skeletonHeight = 0f
            var skeletonWidth = 0f
            var useInnateBehavior = true
            var innateAggressiveness = 0.5f
            var innateTakeCoverProbability = 0.5f
            var innateIsStationary = false
            var firstPersonHands = ""
            var humanAnimOverrideDefId = 0
            var humanLoiterCollectionDefId = 0
            var deathSoundPresetId = 0
            val dialogList = mutableListOf<Dialogue>()

            objDataChunk.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> smartParentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                MICROCHUNKID_DEF_TURN_RATE -> turnRate = microData.toLeFloat()
                                MICROCHUNKID_DEF_JUMP_VELOCITY -> jumpVelocity = microData.toLeFloat()
                                MICROCHUNKID_DEF_SKELETON_HEIGHT -> skeletonHeight = microData.toLeFloat()
                                MICROCHUNKID_DEF_SKELETON_WIDTH -> skeletonWidth = microData.toLeFloat()
                                MICROCHUNKID_DEF_USE_INNATE_BEHAVIOR -> useInnateBehavior = microData.toBool()
                                MICROCHUNKID_DEF_INNATE_AGGRESSIVENESS -> innateAggressiveness = microData.toLeFloat()
                                MICROCHUNKID_DEF_INNATE_TAKE_COVER_PROB -> innateTakeCoverProbability = microData.toLeFloat()
                                MICROCHUNKID_DEF_INNATE_IS_STATIONARY -> innateIsStationary = microData.toBool()
                                MICROCHUNKID_DEF_FIRST_PERSON_HANDS -> firstPersonHands = microData.toNullTerminatedString()
                                MICROCHUNKID_DEF_ORATOR_TYPE -> { /* legacy, ignored at soldier level */ }
                                MICROCHUNKID_DEF_HUMAN_ANIM_OVERRIDE_DEF_ID -> humanAnimOverrideDefId = microData.toLeInt()
                                MICROCHUNKID_DEF_HUMAN_LOITER_COLLECTION_DEF_ID -> humanLoiterCollectionDefId = microData.toLeInt()
                                MICROCHUNKID_DEF_DEATH_SOUND_PRESET -> deathSoundPresetId = microData.toLeInt()
                            }
                        }
                    }
                    CHUNKID_DEF_DIALOG_ENTRY -> {
                        if (dialogList.size < DIALOG_MAX) {
                            dialogList += Dialogue.load(child)
                        }
                    }
                }
            }

            // --- SmartGameObjDef layer ---
            val smartChunk = smartParentChunk ?: return null
            val (smartData, armedParentChunk) = SmartGameObjDefData.load(smartChunk) ?: return null

            // --- ArmedGameObjDef layer ---
            val armedChunk = armedParentChunk ?: return null
            val (armedData, physParentChunk) = ArmedGameObjDefData.load(armedChunk) ?: return null

            // --- PhysicalGameObjDef layer ---
            val physChunk = physParentChunk ?: return null
            val (physData, damageableParentChunk) = PhysicalGameObjDefData.load(physChunk) ?: return null

            // --- DamageableGameObjDef layer ---
            val damageableChunk = damageableParentChunk ?: return null
            val (damageableData, scriptableParentChunk) = DamageableGameObjDefData.load(damageableChunk) ?: return null

            // --- ScriptableGameObjDef layer ---
            val scriptableChunk = scriptableParentChunk ?: return null
            val (scriptableData, _) = ScriptableGameObjDefData.load(scriptableChunk) ?: return null

            return ParsedSoldierFields(
                scriptable = scriptableData,
                damageable = damageableData,
                physical = physData,
                armed = armedData,
                smart = smartData,
                turnRate = turnRate,
                jumpVelocity = jumpVelocity,
                skeletonHeight = skeletonHeight,
                skeletonWidth = skeletonWidth,
                useInnateBehavior = useInnateBehavior,
                innateAggressiveness = innateAggressiveness,
                innateTakeCoverProbability = innateTakeCoverProbability,
                innateIsStationary = innateIsStationary,
                firstPersonHands = firstPersonHands,
                humanAnimOverrideDefId = humanAnimOverrideDefId,
                humanLoiterCollectionDefId = humanLoiterCollectionDefId,
                deathSoundPresetId = deathSoundPresetId,
                dialogList = dialogList,
            )
        }
    }
}

/** Internal parsed-fields holder used by SoldierGameObjDef subclass loaders. */
internal data class ParsedSoldierFields(
    val scriptable: ScriptableGameObjDefData,
    val damageable: DamageableGameObjDefData,
    val physical: PhysicalGameObjDefData,
    val armed: ArmedGameObjDefData,
    val smart: SmartGameObjDefData,
    val turnRate: Float,
    val jumpVelocity: Float,
    val skeletonHeight: Float,
    val skeletonWidth: Float,
    val useInnateBehavior: Boolean,
    val innateAggressiveness: Float,
    val innateTakeCoverProbability: Float,
    val innateIsStationary: Boolean,
    val firstPersonHands: String,
    val humanAnimOverrideDefId: Int,
    val humanLoiterCollectionDefId: Int,
    val deathSoundPresetId: Int,
    val dialogList: List<Dialogue>,
)
