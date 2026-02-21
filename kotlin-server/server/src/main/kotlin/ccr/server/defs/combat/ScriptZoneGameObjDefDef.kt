package ccr.server.defs.combat

import ccr.server.defs.DefinitionClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of ScriptZoneGameObjDef (Combat/scriptzone.cpp).
 *
 * C++ hierarchy: ScriptZoneGameObjDef : BaseGameObjDef : DefinitionClass
 *
 * C++ defaults (constructor):
 *   Color(0, 0.7F, 0), CheckStarsOnly(true), ZoneType(TYPE_DEFAULT=0), IsEnvironmentZone(false)
 */
class ScriptZoneGameObjDef(
    name: String,
    id: UInt,
    classId: UInt,
    val colorR: Float = 0f,
    val colorG: Float = 0.7f,
    val colorB: Float = 0f,
    val checkStarsOnly: Boolean = true,
    val zoneType: Int = 0,
    val isEnvironmentZone: Boolean = false,
) : DefinitionClass(name, id, classId) {
    companion object {
        const val CLASS_ID: UInt = 0x300Eu
    }
}

// Chunk IDs from scriptzone.cpp local enum (line 87)
private const val CHUNKID_DEF_VARIABLES = 1111991133u

// Micro-chunk IDs
private const val MICROCHUNKID_DEF_ZONE_COLOR = 2
private const val MICROCHUNKID_DEF_CHECK_STARS_ONLY = 3
private const val MICROCHUNKID_DEF_ZONE_TYPE = 4
private const val MICROCHUNKID_DEF_IS_ENVIRONMENT_ZONE = 5

fun parseScriptZoneGameObjDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): ScriptZoneGameObjDef {
    val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
        ?: return ScriptZoneGameObjDef(name = name, id = id, classId = classId)

    // Color is a Vector3 (3 floats = 12 bytes)
    val colorBytes = vars.findMicroChunk(MICROCHUNKID_DEF_ZONE_COLOR)
    val colorR: Float
    val colorG: Float
    val colorB: Float
    if (colorBytes != null && colorBytes.size >= 12) {
        val buf = ByteBuffer.wrap(colorBytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
        colorR = buf.getFloat(0)
        colorG = buf.getFloat(4)
        colorB = buf.getFloat(8)
    } else {
        colorR = 0f
        colorG = 0.7f
        colorB = 0f
    }

    val checkStarsOnly = vars.findMicroChunk(MICROCHUNKID_DEF_CHECK_STARS_ONLY)?.let {
        if (it.isNotEmpty()) it[0] != 0.toByte() else null
    } ?: true

    val zoneType = vars.findMicroChunk(MICROCHUNKID_DEF_ZONE_TYPE)?.let {
        if (it.size >= 4) ByteBuffer.wrap(it, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int else null
    } ?: 0

    val isEnvironmentZone = vars.findMicroChunk(MICROCHUNKID_DEF_IS_ENVIRONMENT_ZONE)?.let {
        if (it.isNotEmpty()) it[0] != 0.toByte() else null
    } ?: false

    return ScriptZoneGameObjDef(
        name = name,
        id = id,
        classId = classId,
        colorR = colorR,
        colorG = colorG,
        colorB = colorB,
        checkStarsOnly = checkStarsOnly,
        zoneType = zoneType,
        isEnvironmentZone = isEnvironmentZone,
    )
}
