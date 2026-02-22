package ccr.server.defs

import ccr.server.defs.phys.AnimCollisionManagerDef
import ccr.server.defs.phys.ProjectorManagerDef
import ccr.server.defs.phys.parseStaticAnimPhysDefClass
import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of ShakeableStaticPhysDefClass (wwphys/shakeablestaticphys.cpp).
 *
 * A static physics object that plays a shake animation when collided with.
 * Extends StaticAnimPhysDefClass -- has NO additional fields of its own.
 *
 * Chunk layout inside OBJDATA:
 *   [SHAKEABLESTATICPHYSDEF_CHUNK_STATICANIMPHYSDEF = 7311734]
 *     -> StaticAnimPhysDefClass::Save() output (all parent fields nested within)
 */
data class ShakeableStaticPhysDefClass(
    val name: String,
    val id: UInt,
    // PhysDefClass fields
    val modelName: String = "NULL",
    val isPreLit: Boolean = false,
    // StaticPhysDefClass fields
    val isNonOccluder: Boolean = true,
    // StaticAnimPhysDefClass fields
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
) {
    companion object {
        /** CLASSID_SHAKEABLESTATICPHYSDEF (wwphysids.h enum offset 15 from CLASSID_PHYSICS=0x9000) */
        const val CHUNK_ID: UInt = 0x00020510u  // PHYSICS_CHUNKID_SHAKEABLESTATICPHYSDEF

        // Chunk IDs from shakeablestaticphys.cpp
        private const val CHUNKID_PARENT = 7311734u

        fun load(objDataChunk: ChunkReader, name: String, id: UInt): ShakeableStaticPhysDefClass {
            // Parse all parent StaticAnimPhysDefClass fields from the parent chunk
            val parentChunk = objDataChunk.findChunk(CHUNKID_PARENT)
            val parent = if (parentChunk != null) {
                parseStaticAnimPhysDefClass(parentChunk, name, id, CHUNK_ID)
            } else {
                null
            }

            return ShakeableStaticPhysDefClass(
                name = name,
                id = id,
                modelName = parent?.modelName ?: "NULL",
                isPreLit = parent?.isPreLit ?: false,
                isNonOccluder = parent?.isNonOccluder ?: true,
                shadowDynamicObjs = parent?.shadowDynamicObjs ?: false,
                shadowIsAdditive = parent?.shadowIsAdditive ?: false,
                shadowIgnoresZRotation = parent?.shadowIgnoresZRotation ?: true,
                shadowNearZ = parent?.shadowNearZ ?: 0.5f,
                shadowFarZ = parent?.shadowFarZ ?: 5.0f,
                shadowIntensity = parent?.shadowIntensity ?: 0.5f,
                doesCollideInPathfind = parent?.doesCollideInPathfind ?: false,
                isCosmetic = parent?.isCosmetic ?: false,
                animManagerDef = parent?.animManagerDef ?: AnimCollisionManagerDef(),
                projectorManagerDef = parent?.projectorManagerDef ?: ProjectorManagerDef(),
            )
        }
    }
}
