package ccr.physics.defs

import ccr.server.core.ChunkReader

// ---------------------------------------------------------------------------
// DefenseObjectDef — mirroring the C++ DefenseObjectDefClass save format
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

        private fun ByteArray.toLeFloat(): Float =
            if (size < 4) 0f
            else java.nio.ByteBuffer.wrap(this, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).float

        private fun ByteArray.toLeInt(): Int =
            if (size < 4) 0
            else java.nio.ByteBuffer.wrap(this, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
    }
}
