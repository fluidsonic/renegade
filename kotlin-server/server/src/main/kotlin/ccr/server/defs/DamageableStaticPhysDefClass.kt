package ccr.server.defs

import ccr.server.defs.phys.AnimCollisionManagerDef
import ccr.server.defs.phys.ProjectorManagerDef
import ccr.server.defs.phys.parseStaticAnimPhysDefClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of DamageableStaticPhysDefClass (Combat/damageablestaticphys.h).
 *
 * C++ hierarchy: DamageableStaticPhysDefClass : StaticAnimPhysDefClass : StaticPhysDefClass : PhysDefClass : DefinitionClass
 *
 * Chunk layout inside OBJDATA:
 *   [7311734] parent (StaticAnimPhysDefClass::Save output)
 *     [0x55110100] parent (StaticPhysDefClass::Save)
 *       [0x01070002] parent (PhysDefClass::Save)
 *         [0x055FFE07] DefinitionClass (name, id)
 *         [0x055FFE08] PhysDef variables (modelName, isPreLit)
 *       [0x01070003] StaticPhysDef variables (isNonOccluder)
 *     [0x55110101] ProjectorManagerDef
 *     [0x55110102] StaticAnimPhysDef variables
 *     [0x55110103] AnimCollisionManagerDef
 *   [7311735] variables — micro-chunks for damage behavior fields
 *   [7311736] defenseObjectDef (DefenseObjectDefClass::Save output)
 */
class DamageableStaticPhysDefClass(
    name: String,
    id: UInt,
    classId: UInt,
    // PhysDefClass
    val modelName: String = "NULL",
    val isPreLit: Boolean = false,
    // StaticPhysDefClass
    val isNonOccluder: Boolean = true,
    // StaticAnimPhysDefClass
    val shadowDynamicObjs: Boolean = false,
    val shadowIsAdditive: Boolean = false,
    val shadowIgnoresZRotation: Boolean = true,
    val shadowNearZ: Float = 0.5f,
    val shadowFarZ: Float = 5.0f,
    val shadowIntensity: Float = 0.5f,
    val doesCollideInPathfind: Boolean = false,
    val isCosmetic: Boolean = false,
    val animManagerDef: AnimCollisionManagerDef = AnimCollisionManagerDef(),
    val projectorManagerDef: ProjectorManagerDef = ProjectorManagerDef(),
    // DamageableStaticPhysDef own fields
    val killedExplosion: Int = 0,
    val liveLoopStart: Int = 0,
    val liveLoopEnd: Int = 0,
    val liveTwitchStart: Int = 0,
    val liveTwitchEnd: Int = 0,
    val deathTransitionStart: Int = 0,
    val deathTransitionEnd: Int = 0,
    val deadLoopStart: Int = 0,
    val deadLoopEnd: Int = 0,
    val deadTwitchStart: Int = 0,
    val deadTwitchEnd: Int = 0,
    val playTwitchesToCompletion: Boolean = false,
    // Embedded DefenseObjectDef
    val defenseObjectDef: DefenseObjectDef = DefenseObjectDef(),
) : DefinitionClass(name, id, classId) {

    companion object {
        // Chunk IDs from damageablestaticphys.cpp
        private val CHUNKID_STATICANIMPHYSDEF = 7311734u
        private val CHUNKID_VARIABLES = 7311735u
        private val CHUNKID_DEFENSEOBJECTDEF = 7311736u

        // Micro-chunk IDs (enum starting at 0x00)
        private const val VARID_KILLEDEXPLOSION = 0x00
        // 0x01 = RESETAFTERANIM (unused in save/load)
        private const val VARID_LIVELOOPSTART = 0x02
        private const val VARID_LIVELOOPEND = 0x03
        private const val VARID_LIVETWITCHSTART = 0x04
        private const val VARID_LIVETWITCHEND = 0x05
        private const val VARID_DEATHTRANSITIONSTART = 0x06
        private const val VARID_DEATHTRANSITIONEND = 0x07
        private const val VARID_DEADLOOPSTART = 0x08
        private const val VARID_DEADLOOPEND = 0x09
        private const val VARID_DEADTWITCHSTART = 0x0A
        private const val VARID_DEADTWITCHEND = 0x0B
        private const val VARID_PLAYTWITCHESTOCOMPLETION = 0x0C

        // DefinitionClass base
        private val CHUNKID_BASE_VARIABLES = 0x00000100u
        private const val BASE_VARID_INSTANCEID = 0x01
        private const val BASE_VARID_NAME = 0x03

        fun load(classId: UInt, objDataChunk: ChunkReader): DamageableStaticPhysDefClass? {
            // DefinitionClass base (deep in parent chain)
            val baseVarsChunk = objDataChunk.findChunkRecursive(CHUNKID_BASE_VARIABLES) ?: return null
            val idBytes = baseVarsChunk.findMicroChunk(BASE_VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
            val nameBytes = baseVarsChunk.findMicroChunk(BASE_VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)

            // Parse StaticAnimPhysDefClass parent chain
            val parentChunk = objDataChunk.findChunk(CHUNKID_STATICANIMPHYSDEF)
            val parentObj = if (parentChunk != null) {
                parseStaticAnimPhysDefClass(parentChunk, name, id, classId)
            } else null

            // Own variables
            var killedExplosion = 0
            var liveLoopStart = 0
            var liveLoopEnd = 0
            var liveTwitchStart = 0
            var liveTwitchEnd = 0
            var deathTransitionStart = 0
            var deathTransitionEnd = 0
            var deadLoopStart = 0
            var deadLoopEnd = 0
            var deadTwitchStart = 0
            var deadTwitchEnd = 0
            var playTwitchesToCompletion = false

            objDataChunk.findChunk(CHUNKID_VARIABLES)?.forEachMicroChunk { mcId, data ->
                when (mcId) {
                    VARID_KILLEDEXPLOSION -> if (data.size >= 4) killedExplosion = data.leInt()
                    VARID_LIVELOOPSTART -> if (data.size >= 4) liveLoopStart = data.leInt()
                    VARID_LIVELOOPEND -> if (data.size >= 4) liveLoopEnd = data.leInt()
                    VARID_LIVETWITCHSTART -> if (data.size >= 4) liveTwitchStart = data.leInt()
                    VARID_LIVETWITCHEND -> if (data.size >= 4) liveTwitchEnd = data.leInt()
                    VARID_DEATHTRANSITIONSTART -> if (data.size >= 4) deathTransitionStart = data.leInt()
                    VARID_DEATHTRANSITIONEND -> if (data.size >= 4) deathTransitionEnd = data.leInt()
                    VARID_DEADLOOPSTART -> if (data.size >= 4) deadLoopStart = data.leInt()
                    VARID_DEADLOOPEND -> if (data.size >= 4) deadLoopEnd = data.leInt()
                    VARID_DEADTWITCHSTART -> if (data.size >= 4) deadTwitchStart = data.leInt()
                    VARID_DEADTWITCHEND -> if (data.size >= 4) deadTwitchEnd = data.leInt()
                    VARID_PLAYTWITCHESTOCOMPLETION -> if (data.isNotEmpty()) playTwitchesToCompletion = data[0] != 0.toByte()
                }
            }

            // DefenseObjectDef (embedded)
            val defObjChunk = objDataChunk.findChunk(CHUNKID_DEFENSEOBJECTDEF)
            val defenseObjectDef = if (defObjChunk != null) DefenseObjectDef.load(defObjChunk) else DefenseObjectDef()

            return DamageableStaticPhysDefClass(
                name = name,
                id = id,
                classId = classId,
                modelName = parentObj?.modelName ?: "NULL",
                isPreLit = parentObj?.isPreLit ?: false,
                isNonOccluder = parentObj?.isNonOccluder ?: true,
                shadowDynamicObjs = parentObj?.shadowDynamicObjs ?: false,
                shadowIsAdditive = parentObj?.shadowIsAdditive ?: false,
                shadowIgnoresZRotation = parentObj?.shadowIgnoresZRotation ?: true,
                shadowNearZ = parentObj?.shadowNearZ ?: 0.5f,
                shadowFarZ = parentObj?.shadowFarZ ?: 5.0f,
                shadowIntensity = parentObj?.shadowIntensity ?: 0.5f,
                doesCollideInPathfind = parentObj?.doesCollideInPathfind ?: false,
                isCosmetic = parentObj?.isCosmetic ?: false,
                animManagerDef = parentObj?.animManagerDef ?: AnimCollisionManagerDef(),
                projectorManagerDef = parentObj?.projectorManagerDef ?: ProjectorManagerDef(),
                killedExplosion = killedExplosion,
                liveLoopStart = liveLoopStart,
                liveLoopEnd = liveLoopEnd,
                liveTwitchStart = liveTwitchStart,
                liveTwitchEnd = liveTwitchEnd,
                deathTransitionStart = deathTransitionStart,
                deathTransitionEnd = deathTransitionEnd,
                deadLoopStart = deadLoopStart,
                deadLoopEnd = deadLoopEnd,
                deadTwitchStart = deadTwitchStart,
                deadTwitchEnd = deadTwitchEnd,
                playTwitchesToCompletion = playTwitchesToCompletion,
                defenseObjectDef = defenseObjectDef,
            )
        }

        private fun ByteArray.leInt(): Int =
            ByteBuffer.wrap(this, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
}
