package ccr.server.level.ldd

import ccr.server.defs.readMicroInt
import ccr.server.level.ChunkIds
import ccr.server.level.Matrix3D
import ccr.server.mix.ChunkReader

object GameObjectFactory {

    // BaseGameObj save chunk IDs (basegameobj.cpp)
    private const val BASEGAMEOBJ_CHUNKID_VARIABLES = 910991407u
    private const val MICRO_DEFINITION_ID = 2  // MICROCHUNKID_DEFINITION_ID
    private const val MICRO_INSTANCE_ID = 3    // MICROCHUNKID_INSTANCE_ID

    fun load(factoryChunkId: UInt, objDataReader: ChunkReader): LoadedGameObject {
        val (defId, transform, networkId) = extractBaseFields(objDataReader)

        return when {
            ChunkIds.isBuilding(factoryChunkId) -> LoadedBuildingGameObj(
                definitionId = defId,
                transform = transform,
                networkId = networkId,
                teamId = 0,
                factoryChunkId = factoryChunkId,
                isPowerOn = true,
                sphereCenter = ccr.server.level.Vector3(0f, 0f, 0f),
                sphereRadius = 10f,
            )
            factoryChunkId == ChunkIds.GAMEOBJ_SOLDIER -> LoadedSoldierGameObj(defId, transform, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_VEHICLE -> LoadedVehicleGameObj(defId, transform, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SIMPLE -> LoadedSimpleGameObj(defId, transform, networkId)
            factoryChunkId == ChunkIds.GAMEOBJ_SCRIPTZONE -> LoadedScriptZoneGameObj(defId, transform, networkId)
            else -> UnknownGameObj(defId, transform, networkId, factoryChunkId)
        }
    }

    /**
     * Extracts definitionId and instanceId from the BaseGameObj save hierarchy.
     *
     * The C++ save chain nests CHUNKID_PARENT containers down to BaseGameObj::Save,
     * which writes CHUNKID_VARIABLES (910991407) with micro-chunks:
     *   micro 2 = definition_id, micro 3 = instance_id (game object ID)
     *
     * Transform is not extracted here (stored in physics subsystem); defaults to IDENTITY.
     */
    private fun extractBaseFields(objDataReader: ChunkReader): Triple<Int, Matrix3D, Int> {
        var defId = 0
        var instanceId = 0

        val baseVarsChunk = objDataReader.findChunkRecursive(BASEGAMEOBJ_CHUNKID_VARIABLES)
        if (baseVarsChunk != null) {
            defId = baseVarsChunk.readMicroInt(MICRO_DEFINITION_ID) ?: 0
            instanceId = baseVarsChunk.readMicroInt(MICRO_INSTANCE_ID) ?: 0
        }

        return Triple(defId, Matrix3D.IDENTITY, instanceId)
    }
}
