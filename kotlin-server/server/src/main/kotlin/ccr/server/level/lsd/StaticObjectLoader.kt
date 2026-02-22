package ccr.server.level.lsd

import ccr.server.level.ChunkIds
import ccr.server.level.Matrix3D
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
        // Inside the wrapper is a factory chunk (keyed by factory chunk ID, e.g. 0x00020109).
        // Inside that: OBJPOINTER (skip) + OBJDATA (StaticPhysClass::Save output).
        //
        // OBJDATA structure (staticphyssaveload.cpp, physclass.cpp, renderobj.cpp):
        //   STATICPHYS_CHUNK_PHYS (0x00DC2F94)
        //     PHYS_CHUNK_VARIABLES (0x00660055)  -- micro: 0x03=flags, 0x04=name, 0x06=definitionId
        //     PHYS_CHUNK_MODEL (0x00660056)
        //       WW3D_PERSIST_CHUNKID_RENDEROBJ (0x00010000)
        //         RENDOBJFACTORY_CHUNKID_VARIABLES (0x00555040)
        //           micro 0x01 = modelName (string)
        //           micro 0x02 = transform (48 bytes = Matrix3D row-major)
        var modelName = ""
        var transform = Matrix3D.IDENTITY
        var definitionId = 0

        objectWrapperChunk.forEachChunk { _, _, factoryChunk ->
            val objData = factoryChunk.findChunk(ChunkIds.SIMPLEFACTORY_CHUNKID_OBJDATA)
                ?: return@forEachChunk
            val physChunk = objData.findChunk(ChunkIds.STATICPHYS_CHUNK_PHYS)
                ?: return@forEachChunk

            physChunk.findChunk(ChunkIds.PHYS_CHUNK_VARIABLES)?.forEachMicroChunk { microId, bytes ->
                if (microId == 0x06 && bytes.size >= 4) {
                    definitionId = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
                }
            }

            physChunk.findChunk(ChunkIds.PHYS_CHUNK_MODEL)
                ?.findChunk(ChunkIds.WW3D_PERSIST_CHUNKID_RENDEROBJ)
                ?.findChunk(ChunkIds.RENDOBJFACTORY_CHUNKID_VARIABLES)
                ?.forEachMicroChunk { microId, bytes ->
                    when (microId) {
                        0x01 -> {
                            val nullIdx = bytes.indexOfFirst { it == 0.toByte() }
                            modelName = String(bytes, 0, if (nullIdx < 0) bytes.size else nullIdx, Charsets.ISO_8859_1)
                        }
                        0x02 -> {
                            if (bytes.size >= 48) {
                                val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                                transform = Matrix3D(FloatArray(12) { bb.getFloat(it * 4) })
                            }
                        }
                    }
                }
        }

        if (modelName.isEmpty()) return null
        return StaticPhysObject(definitionId, transform, modelName)
    }

    private fun parseStaticLight(lightWrapperChunk: ChunkReader): StaticLight? {
        // Inside the wrapper is a factory chunk (keyed by factory chunk ID).
        // Inside that: OBJPOINTER + OBJDATA.
        // TODO: parse OBJDATA once we know the LightPhysClass binary format.
        return null
    }
}
