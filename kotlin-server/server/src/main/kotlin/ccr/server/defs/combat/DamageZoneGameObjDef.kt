package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.defs.readMicroFloat
import ccr.server.defs.readMicroFloatAt
import ccr.server.defs.readMicroInt
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of DamageZoneGameObjDef (Combat/damagezone.cpp).
 *
 * C++ hierarchy: DamageZoneGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (damagezone.cpp constructor):
 *   DamageRate(10), DamageWarhead(1), Color(0.7, 0, 0)
 */
class DamageZoneGameObjDef(
    name: String,
    id: UInt,
    classId: UInt,
    val damageRate: Float = 10f,
    val damageWarhead: Int = 1,
    val colorR: Float = 0.7f,
    val colorG: Float = 0f,
    val colorB: Float = 0f,
) : DefinitionClass(name, id, classId) {

    companion object {
        /** CLASSID_GAME_OBJECT_DEF_DAMAGE_ZONE = CLASSID_GAME_OBJECTS + 18 = 0x3012 */
        const val CLASS_ID: UInt = 0x3012u
    }
}

// Chunk IDs from damagezone.cpp local enum (CHUNKID_DEF_PARENT = 626000947)
private const val CHUNKID_DEF_VARIABLES = 626000948u

// Micro-chunk IDs (1 = XXXMICROCHUNKID_DEF_DAMAGE_TYPE, deprecated)
private const val MICROCHUNKID_DEF_ZONE_COLOR = 2
private const val MICROCHUNKID_DEF_DAMAGE_RATE = 3
private const val MICROCHUNKID_DEF_DAMAGE_WARHEAD = 4

/**
 * Parses a DamageZoneGameObjDef from the OBJDATA chunk.
 * [name], [id], and [classId] are already extracted by the definition DB reader.
 */
fun parseDamageZoneGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): DamageZoneGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return DamageZoneGameObjDef(name = name, id = id, classId = classId)

    // Color is a Vector3 (3 floats, 12 bytes in one micro-chunk)
    val colorR = vars.readMicroFloatAt(MICROCHUNKID_DEF_ZONE_COLOR, 0) ?: 0.7f
    val colorG = vars.readMicroFloatAt(MICROCHUNKID_DEF_ZONE_COLOR, 4) ?: 0f
    val colorB = vars.readMicroFloatAt(MICROCHUNKID_DEF_ZONE_COLOR, 8) ?: 0f

    return DamageZoneGameObjDef(
        name = name,
        id = id,
        classId = classId,
        damageRate = vars.readMicroFloat(MICROCHUNKID_DEF_DAMAGE_RATE) ?: 10f,
        damageWarhead = vars.readMicroInt(MICROCHUNKID_DEF_DAMAGE_WARHEAD) ?: 1,
        colorR = colorR,
        colorG = colorG,
        colorB = colorB,
    )
}
