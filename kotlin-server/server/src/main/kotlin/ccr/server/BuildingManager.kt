package ccr.server

import ccr.math.Vector3
import ccr.net.connection.RemoteHost
import ccr.net.replication.NetworkObjectManager
import ccr.server.level.ChunkIds
import ccr.server.level.LoadedLevel
import ccr.server.level.ldd.LoadedBuildingGameObj
import ccr.server.net.*

/**
 * Manages building network objects for a map.
 * Port of C++ BaseControllerClass and CNCModeSettings building lifecycle.
 *
 * At startup: creates BuildingGameObj subtypes for each LoadedBuildingGameObj,
 * registers them with NetworkObjectManager, creates two BaseControllerClass singletons.
 *
 * On client connect: caller sends all buildings + base controllers.
 */
class BuildingManager(private val server: GameServer, level: LoadedLevel) {

    val buildings = mutableListOf<BuildingGameObj>()

    // Maps each BuildingGameObj to the playerType from its LDD data.
    private val buildingTeams = mutableMapOf<BuildingGameObj, Int>()

    val baseControllerNod = BaseControllerClass(playerType = 0)
    val baseControllerGdi = BaseControllerClass(playerType = 1)

    init {
        NetworkObjectManager.registerObject(baseControllerNod, GameServer.NET_ID_BASE_CONTROLLER_NOD)
        NetworkObjectManager.registerObject(baseControllerGdi, GameServer.NET_ID_BASE_CONTROLLER_GDI)

        val loadedBuildings = level.dynamicData.gameObjects.filterIsInstance<LoadedBuildingGameObj>()
        println("[BUILDING] found ${loadedBuildings.size} buildings in LDD")

        for (lb in loadedBuildings) {
            val building = createBuilding(lb) ?: continue
            NetworkObjectManager.registerObject(building, lb.networkId)
            buildings.add(building)
            buildingTeams[building] = lb.playerType
            println("[BUILDING] registered ${building::class.simpleName} networkId=${lb.networkId} defId=${lb.definitionId} playerType=${lb.playerType}")
        }

        println("[BUILDING] registered ${buildings.size} buildings, 2 base controllers")
    }

    fun sendToClient(host: RemoteHost) {
        server.sendGameNetObj(host) { bs ->
            NetworkObjectPacketWriter.writeCreation(bs, baseControllerNod, GameServer.NET_ID_BASE_CONTROLLER_NOD)
        }
        server.sendGameNetObj(host) { bs ->
            NetworkObjectPacketWriter.writeCreation(bs, baseControllerGdi, GameServer.NET_ID_BASE_CONTROLLER_GDI)
        }
        for (building in buildings) {
            server.sendGameNetObj(host) { bs ->
                NetworkObjectPacketWriter.writeCreation(bs, building, building.networkId)
            }
        }
    }

    fun isBaseDestroyed(teamNumber: Int): Boolean {
        val teamBuildings = buildings.filter { getTeamForBuilding(it) == teamNumber }
        if (teamBuildings.isEmpty()) return false
        return teamBuildings.all { it.isDestroyed }
    }

    private fun getTeamForBuilding(building: BuildingGameObj): Int {
        return buildingTeams[building] ?: 0
    }

    private fun createBuilding(lb: LoadedBuildingGameObj): BuildingGameObj? {
        val pos = Vector3(lb.transform.position.x, lb.transform.position.y, lb.transform.position.z)
        val sphereCenter = Vector3(lb.collectionSphere.center.x, lb.collectionSphere.center.y, lb.collectionSphere.center.z)
        val radius = lb.collectionSphere.radius

        if (!ChunkIds.isBuilding(lb.factoryChunkId)) return null

        return when (lb.factoryChunkId) {
            ChunkIds.GAMEOBJ_BUILDING_POWERPLANT ->
                PowerPlantGameObj(lb.definitionId, pos, sphereCenter, radius, isPowerOn = lb.isPowerOn)

            ChunkIds.GAMEOBJ_BUILDING_REFINERY ->
                RefineryGameObj(lb.definitionId, pos, sphereCenter, radius)

            ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY ->
                SoldierFactoryGameObj(lb.definitionId, pos, sphereCenter, radius)

            ChunkIds.GAMEOBJ_BUILDING_WARFACTORY ->
                WarFactoryGameObj(lb.definitionId, pos, sphereCenter, radius)

            ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP,
            ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY ->
                VehicleFactoryGameObj(lb.definitionId, pos, sphereCenter, radius)

            ChunkIds.GAMEOBJ_BUILDING_COMCENTER ->
                ComCenterGameObj(lb.definitionId, pos, sphereCenter, radius)

            else -> BuildingGameObj(lb.definitionId, pos, sphereCenter, radius)
        }
    }
}
