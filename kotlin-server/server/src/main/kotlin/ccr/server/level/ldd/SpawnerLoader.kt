package ccr.server.level.ldd

import ccr.server.level.ChunkIds
import ccr.server.level.Matrix3D
import ccr.server.level.toInt32
import ccr.server.level.toMatrix3D
import ccr.server.mix.ChunkReader

object SpawnerLoader {

    fun load(spawnersChunk: ChunkReader): List<LoadedSpawner> {
        val spawners = mutableListOf<LoadedSpawner>()

        // SpawnManager::Save writes one SPAWNER_CHUNKID_DATA wrapper per spawner, plus a
        // SPAWNER_CHUNKID_VARIABLES_MGR chunk for the SpawnManager's own timer (skipped here).
        // Inside each SPAWNER_CHUNKID_DATA wrapper, SpawnerClass::Save writes
        // SPAWNER_CHUNKID_PARENT + SPAWNER_CHUNKID_VARIABLES.
        spawnersChunk.forEachChunk { chunkId, _, dataChunk ->
            if (chunkId != ChunkIds.SPAWNER_CHUNKID_DATA) return@forEachChunk

            // Inside SPAWNER_CHUNKID_DATA, SpawnerClass writes two sibling chunks:
            //   SPAWNER_CHUNKID_PARENT    — PersistClass data (not needed here)
            //   SPAWNER_CHUNKID_VARIABLES — the actual spawner fields as micro-chunks
            val varsChunk = dataChunk.findChunk(ChunkIds.SPAWNER_CHUNKID_VARIABLES)
                ?: return@forEachChunk

            var id = 0
            var transform: Matrix3D? = null
            var spawnTransform: Matrix3D? = null
            var definitionId = 0
            var spawnCount = 0
            var enabled = true
            val spawnPoints = mutableListOf<Matrix3D>()
            val scriptNames = mutableListOf<String>()
            val scriptParams = mutableListOf<String>()

            varsChunk.forEachMicroChunk { microId, data ->
                when (microId) {
                    1 -> id = data.toInt32()
                    2 -> transform = data.toMatrix3D()
                    3 -> definitionId = data.toInt32()
                    4 -> spawnCount = data.toInt32()
                    6 -> enabled = data.isNotEmpty() && data[0] != 0.toByte()
                    7 -> spawnPoints.add(data.toMatrix3D())
                    9 -> spawnTransform = data.toMatrix3D()
                    10 -> scriptNames.add(data.toNullTerminatedString())
                    11 -> scriptParams.add(data.toNullTerminatedString())
                }
            }

            val scripts = scriptNames.zip(scriptParams).map { (n, p) -> ScriptAttachment(n, p) }

            spawners.add(
                LoadedSpawner(
                    id = id,
                    transform = transform ?: Matrix3D.IDENTITY,
                    spawnTransform = spawnTransform,
                    definitionId = definitionId,
                    spawnCount = spawnCount,
                    enabled = enabled,
                    spawnPoints = spawnPoints,
                    scripts = scripts,
                )
            )
        }

        return spawners
    }

    private fun ByteArray.toNullTerminatedString(): String {
        val nullIndex = indexOfFirst { it == 0.toByte() }
        val len = if (nullIndex < 0) size else nullIndex
        return String(this, 0, len, Charsets.ISO_8859_1)
    }
}
