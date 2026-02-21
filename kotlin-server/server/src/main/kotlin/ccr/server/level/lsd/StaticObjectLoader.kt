package ccr.server.level.lsd

import ccr.server.level.ChunkIds
import ccr.server.mix.ChunkReader

/**
 * Parses the PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM chunk.
 *
 * Chunk structure (pscene_saveload.cpp):
 *   [0x00020001] PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM
 *     [0x06090609] PSOSSC_CHUNKID_SCENE
 *       [0x00770100] PSCENE_SO_CHUNK_STATIC_OBJECTS
 *         [0x00770101] PSCENE_SO_CHUNK_STATIC_OBJECT  (per object)
 *           [factoryChunkId]  (PersistFactory wrapper)
 *             [0x00100100] SIMPLEFACTORY_CHUNKID_OBJPOINTER — old pointer, skip
 *             [0x00100101] SIMPLEFACTORY_CHUNKID_OBJDATA — object's Save() output
 *         [0x00770102] PSCENE_SO_CHUNK_STATIC_OBJECT_AABLINK — culling linkage, skip
 *       [0x00770200] PSCENE_SO_CHUNK_STATIC_LIGHTS
 *         [0x00770201] PSCENE_SO_CHUNK_STATIC_LIGHT  (per light)
 *           [factoryChunkId]
 *             [0x00100100] OBJPOINTER
 *             [0x00100101] OBJDATA
 *         [0x00770202] PSCENE_SO_CHUNK_STATIC_LIGHT_AABLINK — skip
 *
 * For now we count objects/lights but don't parse their internal Save() data.
 * Full parsing requires knowing each factory's binary format (StaticPhysClass, LightPhysClass).
 */
object StaticObjectLoader {

    fun load(subsystemChunk: ChunkReader): Pair<List<StaticPhysObject>, List<StaticLight>> {
        val sceneChunk = subsystemChunk.findChunk(ChunkIds.PSOSSC_CHUNKID_SCENE)
            ?: return Pair(emptyList(), emptyList())

        val objects = mutableListOf<StaticPhysObject>()
        val lights = mutableListOf<StaticLight>()

        sceneChunk.forEachChunk { chunkId, _, chunkReader ->
            when (chunkId) {
                ChunkIds.PSCENE_SO_CHUNK_STATIC_OBJECTS -> {
                    objects.addAll(loadStaticObjects(chunkReader))
                }
                ChunkIds.PSCENE_SO_CHUNK_STATIC_LIGHTS -> {
                    lights.addAll(loadStaticLights(chunkReader))
                }
            }
        }

        return Pair(objects, lights)
    }

    private fun loadStaticObjects(objectsChunk: ChunkReader): List<StaticPhysObject> {
        val result = mutableListOf<StaticPhysObject>()
        objectsChunk.forEachChunk { chunkId, _, chunkReader ->
            if (chunkId == ChunkIds.PSCENE_SO_CHUNK_STATIC_OBJECT) {
                parseStaticObject(chunkReader)?.let { result.add(it) }
            }
            // PSCENE_SO_CHUNK_STATIC_OBJECT_AABLINK chunks are skipped
        }
        return result
    }

    private fun loadStaticLights(lightsChunk: ChunkReader): List<StaticLight> {
        val result = mutableListOf<StaticLight>()
        lightsChunk.forEachChunk { chunkId, _, chunkReader ->
            if (chunkId == ChunkIds.PSCENE_SO_CHUNK_STATIC_LIGHT) {
                parseStaticLight(chunkReader)?.let { result.add(it) }
            }
            // PSCENE_SO_CHUNK_STATIC_LIGHT_AABLINK chunks are skipped
        }
        return result
    }

    private fun parseStaticObject(objectWrapperChunk: ChunkReader): StaticPhysObject? {
        // Inside the wrapper is a factory chunk (keyed by factory chunk ID).
        // Inside that: OBJPOINTER + OBJDATA.
        // TODO: parse OBJDATA once we know the StaticPhysClass binary format.
        // For now, return null (skip) — objects will be populated in a later step.
        return null
    }

    private fun parseStaticLight(lightWrapperChunk: ChunkReader): StaticLight? {
        // Inside the wrapper is a factory chunk (keyed by factory chunk ID).
        // Inside that: OBJPOINTER + OBJDATA.
        // TODO: parse OBJDATA once we know the LightPhysClass binary format.
        return null
    }
}
