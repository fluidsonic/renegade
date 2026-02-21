package ccr.server.defs

import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Per-state animation data for BuildingAggregateDefClass.
 * Each of the 10 building states (STATE_COUNT from buildingstate.h) has its own animation settings.
 */
data class BuildingStateAnimData(
    val frame0: Int = 0,
    val frame1: Int = 0,
    val animationEnabled: Boolean = true,
)

/**
 * Kotlin representation of BuildingAggregateDefClass (Combat/buildingaggregate.h).
 *
 * C++ hierarchy: BuildingAggregateDefClass : StaticAnimPhysDefClass : StaticPhysDefClass : PhysDefClass : DefinitionClass
 *
 * Chunk layout inside OBJDATA:
 *   [8281441] parent (StaticAnimPhysDefClass::Save output)
 *   [8281442] variables — micro-chunks for animLogicMode, isMCT
 *   [8281443..8281452] per-state animation data (10 states: HEALTH100_POWERON .. DESTROYED_POWEROFF)
 */
data class BuildingAggregateDefClass(
    // DefinitionClass base
    val name: String,
    val definitionId: UInt,
    // BuildingAggregateDef own fields
    val animLogicMode: Int,
    val isMCT: Boolean,
    // Per-state animation data (10 states from BuildingStateClass)
    val stateAnimations: List<BuildingStateAnimData>,
) {
    companion object {
        // BuildingStateClass::STATE_COUNT
        private const val STATE_COUNT = 10

        // Chunk IDs from buildingaggregate.cpp
        private val CHUNKID_STATICANIMPHYS = 8281441u
        private val CHUNKID_VARIABLES = 8281442u
        // Per-state chunk IDs: 8281443 + stateIndex (0..9)
        private val CHUNKID_HEALTH100_POWERON_VARIABLES = 8281443u

        // Micro-chunk IDs (shared between main variables and per-state chunks)
        private const val VARID_ANIMLOGICMODE = 0
        private const val VARID_FRAME0 = 1
        private const val VARID_FRAME1 = 2
        private const val VARID_ANIMATIONENABLED = 3
        private const val VARID_ISMCT = 4

        // AnimLogicMode values
        const val ANIM_LOGIC_LINEAR = 0
        const val ANIM_LOGIC_LOOP = 1
        const val ANIM_LOGIC_SEQUENCE = 2

        // DefinitionClass base
        private val CHUNKID_BASE_VARIABLES = 0x00000100u
        private const val BASE_VARID_INSTANCEID = 0x01
        private const val BASE_VARID_NAME = 0x03

        fun load(objDataChunk: ChunkReader): BuildingAggregateDefClass? {
            // DefinitionClass base (deep in parent chain)
            val baseVarsChunk = objDataChunk.findChunkRecursive(CHUNKID_BASE_VARIABLES) ?: return null
            val idBytes = baseVarsChunk.findMicroChunk(BASE_VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val definitionId = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
            val nameBytes = baseVarsChunk.findMicroChunk(BASE_VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)

            // Own variables (animLogicMode, isMCT)
            var animLogicMode = ANIM_LOGIC_LOOP  // C++ default
            var isMCT = false

            objDataChunk.findChunk(CHUNKID_VARIABLES)?.forEachMicroChunk { id, data ->
                when (id) {
                    VARID_ANIMLOGICMODE -> if (data.size >= 4) animLogicMode = data.leInt()
                    VARID_ISMCT -> if (data.isNotEmpty()) isMCT = data[0] != 0.toByte()
                }
            }

            // Per-state animation data (10 states)
            val states = Array(STATE_COUNT) { BuildingStateAnimData() }
            for (i in 0 until STATE_COUNT) {
                val stateChunkId = CHUNKID_HEALTH100_POWERON_VARIABLES + i.toUInt()
                objDataChunk.findChunk(stateChunkId)?.let { stateChunk ->
                    var frame0 = 0
                    var frame1 = 0
                    var animEnabled = true
                    stateChunk.forEachMicroChunk { id, data ->
                        when (id) {
                            VARID_FRAME0 -> if (data.size >= 4) frame0 = data.leInt()
                            VARID_FRAME1 -> if (data.size >= 4) frame1 = data.leInt()
                            VARID_ANIMATIONENABLED -> if (data.isNotEmpty()) animEnabled = data[0] != 0.toByte()
                        }
                    }
                    states[i] = BuildingStateAnimData(frame0 = frame0, frame1 = frame1, animationEnabled = animEnabled)
                }
            }

            return BuildingAggregateDefClass(
                name = name,
                definitionId = definitionId,
                animLogicMode = animLogicMode,
                isMCT = isMCT,
                stateAnimations = states.toList(),
            )
        }

        private fun ByteArray.leInt(): Int =
            ByteBuffer.wrap(this, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
}
