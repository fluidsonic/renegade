package ccr.server.defs

import ccr.math.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

// C++: DamageZoneGameObjDef : public BaseGameObjDef (damagezone.h / damagezone.cpp)
// C++ hierarchy: DefinitionClass → BaseGameObjDef → DamageZoneGameObjDef
class DamageZoneGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,

    // C++: float DamageRate (initialized to 10)
    val damageRate: Float = 10f,

    // C++: int DamageWarhead (initialized to 1)
    val damageWarhead: Int = 1,

    // C++: Vector3 Color (initialized to (0.7f, 0, 0))
    val color: Vector3 = Vector3(0.7f, 0f, 0f),
) : BaseGameObjDef(name, id, chunkId) {

    companion object {
        // CHUNKID_GAME_OBJECT_DEF_DAMAGE_ZONE = 0x0004012Eu
        const val CHUNK_ID: UInt = 0x0004012Eu

        // C++: enum { CHUNKID_DEF_PARENT = 626000947, CHUNKID_DEF_VARIABLES, ... }
        private const val CHUNKID_DEF_PARENT              = 626000947u
        private const val CHUNKID_DEF_VARIABLES           = 626000948u

        // C++: XXXMICROCHUNKID_DEF_DAMAGE_TYPE = 1 (legacy, not read)
        private const val XXXMICROCHUNKID_DEF_DAMAGE_TYPE = 1
        private const val MICROCHUNKID_DEF_ZONE_COLOR     = 2
        private const val MICROCHUNKID_DEF_DAMAGE_RATE    = 3
        private const val MICROCHUNKID_DEF_DAMAGE_WARHEAD = 4

        // DefinitionClass base chunk/micro IDs
        private const val CHUNKID_BASE_VARIABLES = 0x00000100u
        private const val VARID_INSTANCEID       = 0x01
        private const val VARID_NAME             = 0x03

        fun load(objDataReader: ChunkReader, name: String, id: UInt, chunkId: UInt): DamageZoneGameObjDef {
            val vars = objDataReader.findChunk(CHUNKID_DEF_VARIABLES)
                ?: return DamageZoneGameObjDef(name = name, id = id, chunkId = chunkId)

            val colorBytes = vars.findMicroChunk(MICROCHUNKID_DEF_ZONE_COLOR)
            val color = if (colorBytes != null && colorBytes.size >= 12) {
                val bb = ByteBuffer.wrap(colorBytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
                Vector3(bb.float, bb.float, bb.float)
            } else {
                Vector3(0.7f, 0f, 0f)
            }

            val damageRate = vars.findMicroChunk(MICROCHUNKID_DEF_DAMAGE_RATE)?.let {
                if (it.size >= 4) ByteBuffer.wrap(it, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float else null
            } ?: 10f

            val damageWarhead = vars.findMicroChunk(MICROCHUNKID_DEF_DAMAGE_WARHEAD)?.let {
                if (it.size >= 4) ByteBuffer.wrap(it, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int else null
            } ?: 1

            return DamageZoneGameObjDef(
                name = name,
                id = id,
                chunkId = chunkId,
                damageRate = damageRate,
                damageWarhead = damageWarhead,
                color = color,
            )
        }

        fun load(chunkId: UInt, objDataChunk: ChunkReader): DamageZoneGameObjDef? {
            // --- DamageZoneGameObjDef layer ---
            var damageRate    = 10f
            var damageWarhead = 1
            var color         = Vector3(0.7f, 0f, 0f)
            var parentChunk: ChunkReader? = null

            objDataChunk.forEachChunk { id, _, child ->
                when (id) {
                    CHUNKID_DEF_PARENT -> parentChunk = child
                    CHUNKID_DEF_VARIABLES -> {
                        child.forEachMicroChunk { microId, microData ->
                            when (microId) {
                                XXXMICROCHUNKID_DEF_DAMAGE_TYPE -> { /* legacy — ignored */ }
                                MICROCHUNKID_DEF_ZONE_COLOR -> {
                                    if (microData.size >= 12) {
                                        val bb = ByteBuffer.wrap(microData, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
                                        color = Vector3(bb.float, bb.float, bb.float)
                                    }
                                }
                                MICROCHUNKID_DEF_DAMAGE_RATE    -> {
                                    if (microData.size >= 4)
                                        damageRate = ByteBuffer.wrap(microData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).float
                                }
                                MICROCHUNKID_DEF_DAMAGE_WARHEAD -> {
                                    if (microData.size >= 4)
                                        damageWarhead = ByteBuffer.wrap(microData, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
                                }
                            }
                        }
                    }
                }
            }

            // --- BaseGameObjDef → DefinitionClass layer ---
            val baseChunk = parentChunk ?: return null
            val baseVarsChunk = baseChunk.findChunkRecursive(CHUNKID_BASE_VARIABLES) ?: return null

            val idBytes = baseVarsChunk.findMicroChunk(VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val defId = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()

            val nameBytes = baseVarsChunk.findMicroChunk(VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val nameLen = if (nullIdx < 0) nameBytes.size else nullIdx
            val name = String(nameBytes, 0, nameLen, Charsets.ISO_8859_1)

            return DamageZoneGameObjDef(
                name          = name,
                id            = defId,
                chunkId       = chunkId,
                damageRate    = damageRate,
                damageWarhead = damageWarhead,
                color         = color,
            )
        }
    }
}
