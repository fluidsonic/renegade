package ccr.server.defs

import ccr.server.defs.phys.AnimCollisionManagerDef
import ccr.server.defs.phys.ProjectorManagerDef
import ccr.server.defs.phys.parseStaticAnimPhysDefClass
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin representation of AccessiblePhysDefClass (wwphys/accessiblephys.h).
 *
 * C++ hierarchy: AccessiblePhysDefClass : StaticAnimPhysDefClass : StaticPhysDefClass : PhysDefClass : DefinitionClass
 *
 * Chunk layout inside OBJDATA:
 *   [0x10311249] parent (StaticAnimPhysDefClass::Save output)
 *     [0x55110100] parent (StaticPhysDefClass::Save)
 *       [0x01070002] parent (PhysDefClass::Save)
 *         [0x055FFE07] DefinitionClass (name, id)
 *         [0x055FFE08] PhysDef variables (modelName, isPreLit)
 *       [0x01070003] StaticPhysDef variables (isNonOccluder)
 *     [0x55110101] ProjectorManagerDef
 *     [0x55110102] StaticAnimPhysDef variables
 *     [0x55110103] AnimCollisionManagerDef
 *   [0x1031124A] variables — micro-chunk: lockCode
 */
class AccessiblePhysDefClass(
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
    // AccessiblePhysDef own field
    val lockCode: Int = 0,
) : DefinitionClass(name, id, classId) {

    companion object {
        // Chunk IDs from accessiblephys.cpp
        internal const val CHUNKID_DEF_PARENT: UInt = 0x10311249u
        internal const val CHUNKID_DEF_VARIABLES: UInt = 0x1031124Au

        // DefinitionClass base
        private val CHUNKID_BASE_VARIABLES = 0x00000100u
        private const val BASE_VARID_INSTANCEID = 0x01
        private const val BASE_VARID_NAME = 0x03

        // Own micro-chunk IDs
        internal const val VARID_DEF_LOCKCODE = 1

        fun load(classId: UInt, objDataChunk: ChunkReader): AccessiblePhysDefClass? {
            // DefinitionClass base (deep in parent chain)
            val baseVarsChunk = objDataChunk.findChunkRecursive(CHUNKID_BASE_VARIABLES) ?: return null
            val idBytes = baseVarsChunk.findMicroChunk(BASE_VARID_INSTANCEID) ?: return null
            if (idBytes.size < 4) return null
            val id = ByteBuffer.wrap(idBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int.toUInt()
            val nameBytes = baseVarsChunk.findMicroChunk(BASE_VARID_NAME) ?: return null
            val nullIdx = nameBytes.indexOfFirst { it == 0.toByte() }
            val name = String(nameBytes, 0, if (nullIdx < 0) nameBytes.size else nullIdx, Charsets.ISO_8859_1)

            // Parse StaticAnimPhysDefClass parent chain
            val parentChunk = objDataChunk.findChunk(CHUNKID_DEF_PARENT)
            val parentObj = if (parentChunk != null) {
                parseStaticAnimPhysDefClass(parentChunk, name, id, classId)
            } else null

            // Own variables
            var lockCode = 0
            objDataChunk.findChunk(CHUNKID_DEF_VARIABLES)?.forEachMicroChunk { mcId, data ->
                if (mcId == VARID_DEF_LOCKCODE && data.size >= 4) {
                    lockCode = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
                }
            }

            return AccessiblePhysDefClass(
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
                lockCode = lockCode,
            )
        }
    }
}
