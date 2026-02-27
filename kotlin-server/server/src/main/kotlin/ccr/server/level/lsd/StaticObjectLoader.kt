package ccr.server.level.lsd

import ccr.server.level.ChunkIds
import ccr.server.level.toInt32
import ccr.server.level.toMatrix3D
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

    /** Known factory chunk IDs we can parse. */
    private val KNOWN_FACTORY_IDS = setOf(
        ChunkIds.PHYSICS_CHUNKID_DOORPHYS,
        ChunkIds.PHYSICS_CHUNKID_ELEVATORPHYS,
        ChunkIds.PHYSICS_CHUNKID_DAMAGEABLESTATICPHYS,
    )

    private fun parseStaticObject(objectWrapperChunk: ChunkReader): StaticPhysObject? {
        // The wrapper contains a single factory chunk whose ID identifies the physics type.
        // Inside: OBJPOINTER (skip) + OBJDATA (the object's Save() output).
        var factoryChunkId: UInt? = null
        var objDataReader: ChunkReader? = null

        objectWrapperChunk.forEachChunk { chunkId, _, chunkReader ->
            // The first (and only) chunk is the factory chunk.
            if (factoryChunkId == null) {
                factoryChunkId = chunkId
                // Inside the factory chunk, find OBJDATA.
                chunkReader.forEachChunk { innerId, _, innerReader ->
                    if (innerId == ChunkIds.SIMPLEFACTORY_CHUNKID_OBJDATA && objDataReader == null) {
                        objDataReader = innerReader
                    }
                }
            }
        }

        val fid = factoryChunkId ?: return null
        if (fid !in KNOWN_FACTORY_IDS) return null
        val objData = objDataReader ?: return null

        // Recursively search OBJDATA for the two chunks we need.
        val physVars = objData.findChunkRecursive(ChunkIds.PHYS_CHUNK_VARIABLES)
        val rendObjVars = objData.findChunkRecursive(ChunkIds.RENDOBJFACTORY_CHUNKID_VARIABLES)

        // Parse PHYS_CHUNK_VARIABLES micro-chunks: 0x06=DefID, 0x07=InstanceID
        var definitionId = 0
        var instanceId = 0
        physVars?.let { vars ->
            vars.findMicroChunk(0x06)?.let { definitionId = it.toInt32() }
            vars.findMicroChunk(0x07)?.let { instanceId = it.toInt32() }
        }

        // Parse RENDOBJFACTORY_CHUNKID_VARIABLES micro-chunks: 0x01=NAME, 0x02=TRANSFORM
        var modelName = ""
        var transform = ccr.server.level.Matrix3D.IDENTITY
        rendObjVars?.let { vars ->
            vars.findMicroChunk(0x01)?.let { modelName = it.toNullTerminatedString() }
            vars.findMicroChunk(0x02)?.let { transform = it.toMatrix3D() }
        }

        println("[STATICOBJ] parsed factoryChunkId=0x${fid.toString(16)} instanceId=$instanceId defId=$definitionId model=$modelName")

        return StaticPhysObject(
            factoryChunkId = fid,
            instanceId = instanceId,
            definitionId = definitionId,
            transform = transform,
            modelName = modelName,
        )
    }

    private fun ByteArray.toNullTerminatedString(): String {
        val nullIndex = indexOfFirst { it == 0.toByte() }
        val len = if (nullIndex < 0) size else nullIndex
        return String(this, 0, len, Charsets.ISO_8859_1)
    }

    private fun parseStaticLight(lightWrapperChunk: ChunkReader): StaticLight? {
        // Inside the wrapper is a factory chunk (keyed by factory chunk ID).
        // Inside that: OBJPOINTER + OBJDATA.
        // TODO: parse OBJDATA once we know the LightPhysClass binary format.
        return null
    }
}
