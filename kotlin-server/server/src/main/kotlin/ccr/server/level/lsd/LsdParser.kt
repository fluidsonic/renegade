package ccr.server.level.lsd

import ccr.server.level.ChunkIds
import ccr.server.level.LevelStaticData
import ccr.server.level.Vector3
import ccr.server.mix.ChunkReader
import ccr.server.mix.extractLevelExtents

/**
 * Parses an .lsd (Level Static Data) file into [LevelStaticData].
 *
 * Top-level chunk structure:
 *   [0x00020000] PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM
 *     [0x04433220] PSDSSC_CHUNKID_SCENE → sunlight, ambient, visibility, culling trees
 *     [0x04433221] PSDSSC_CHUNKID_PATHFIND → pathfinding data (Step 6)
 *   [0x00020001] PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM
 *     [0x06090609] PSOSSC_CHUNKID_SCENE → static objects + lights
 *   [0x00030005] WWAUDIO_STATIC_SAVELOAD → static sound emitters
 *
 * Source: physstaticsavesystem.cpp, pscene_saveload.cpp
 */
object LsdParser {

    fun parse(lsdData: ByteArray): LevelStaticData {
        if (lsdData.isEmpty()) return LevelStaticData()

        val reader = ChunkReader(lsdData)

        val worldExtents = try {
            extractLevelExtents(lsdData)
        } catch (_: Exception) {
            null
        }

        var sunlight: Vector3? = null
        var ambient: Vector3? = null
        val staticObjects = mutableListOf<StaticPhysObject>()
        val staticLights = mutableListOf<StaticLight>()
        var staticSounds = emptyList<StaticSoundEmitter>()

        reader.forEachChunk { chunkId, _, chunkReader ->
            when (chunkId) {
                ChunkIds.PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM -> {
                    chunkReader.forEachChunk { subId, _, subReader ->
                        when (subId) {
                            ChunkIds.PSDSSC_CHUNKID_SCENE -> {
                                sunlight = PhysicsSceneLoader.loadSunlight(subReader)
                                ambient = PhysicsSceneLoader.loadAmbientLight(subReader)
                            }
                            // PSDSSC_CHUNKID_PATHFIND handled by Step 6
                        }
                    }
                }
                ChunkIds.PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM -> {
                    val (objs, lights) = StaticObjectLoader.load(chunkReader)
                    staticObjects.addAll(objs)
                    staticLights.addAll(lights)
                }
                ChunkIds.WWAUDIO_STATIC_SAVELOAD -> {
                    staticSounds = SoundLoader.load(chunkReader)
                }
            }
        }

        return LevelStaticData(
            worldExtents = worldExtents,
            sunlightColor = sunlight,
            ambientLightColor = ambient,
            staticObjects = staticObjects,
            staticLights = staticLights,
            staticSounds = staticSounds,
        )
    }
}
