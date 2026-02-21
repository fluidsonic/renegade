package ccr.server.level.ldd

import ccr.server.level.ChunkIds
import ccr.server.level.Matrix3D
import ccr.server.mix.ChunkReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LddParserTest {

    @Test
    fun `parse empty LDD returns empty dynamic data`() {
        val result = LddParser.parse(ByteArray(0))
        assertNotNull(result)
        assertTrue(result.gameObjects.isEmpty())
        assertTrue(result.spawners.isEmpty())
        assertTrue(result.levelScripts.isEmpty())
        assertEquals("", result.mapFilename)
        assertEquals(0, result.missionDescriptionId)
    }

    @Test
    fun `LoadedGameObject sealed hierarchy works`() {
        val transform = Matrix3D.IDENTITY
        val soldier = LoadedSoldierGameObj(100, transform, 5)
        assertTrue(soldier is LoadedGameObject)
        assertEquals(100, soldier.definitionId)
        assertEquals(5, soldier.networkId)

        val building = LoadedBuildingGameObj(200, transform, 10, teamId = 1)
        assertTrue(building is LoadedGameObject)
        assertEquals(200, building.definitionId)
        assertEquals(1, building.teamId)
    }

    @Test
    fun `isBuilding identifies all building chunk IDs`() {
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_REFINERY))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_POWERPLANT))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_SOLDIERFACTORY))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_VEHICLEFACTORY))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_AIRSTRIP))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_WARFACTORY))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_COMCENTER))
        assertTrue(ChunkIds.isBuilding(ChunkIds.GAMEOBJ_BUILDING_REPAIRBAY))
    }

    @Test
    fun `GameObjectFactory creates correct types from empty objdata`() {
        val emptyObjData = ChunkReader(ByteArray(0))

        val building = GameObjectFactory.load(ChunkIds.GAMEOBJ_BUILDING, emptyObjData)
        assertTrue(building is LoadedBuildingGameObj)
        assertEquals(0, building.definitionId)

        val soldier = GameObjectFactory.load(ChunkIds.GAMEOBJ_SOLDIER, emptyObjData)
        assertTrue(soldier is LoadedSoldierGameObj)

        val vehicle = GameObjectFactory.load(ChunkIds.GAMEOBJ_VEHICLE, emptyObjData)
        assertTrue(vehicle is LoadedVehicleGameObj)

        val simple = GameObjectFactory.load(ChunkIds.GAMEOBJ_SIMPLE, emptyObjData)
        assertTrue(simple is LoadedSimpleGameObj)

        val zone = GameObjectFactory.load(ChunkIds.GAMEOBJ_SCRIPTZONE, emptyObjData)
        assertTrue(zone is LoadedScriptZoneGameObj)

        val unknown = GameObjectFactory.load(0xDEADu, emptyObjData)
        assertTrue(unknown is UnknownGameObj)
        assertEquals(0xDEADu, (unknown as UnknownGameObj).factoryChunkId)
    }

    @Test
    fun `GameObjectFactory creates building subtypes`() {
        val emptyObjData = ChunkReader(ByteArray(0))

        val refinery = GameObjectFactory.load(ChunkIds.GAMEOBJ_BUILDING_REFINERY, emptyObjData)
        assertTrue(refinery is LoadedBuildingGameObj)
        assertEquals(ChunkIds.GAMEOBJ_BUILDING_REFINERY, (refinery as LoadedBuildingGameObj).factoryChunkId)

        val warfactory = GameObjectFactory.load(ChunkIds.GAMEOBJ_BUILDING_WARFACTORY, emptyObjData)
        assertTrue(warfactory is LoadedBuildingGameObj)
        assertEquals(ChunkIds.GAMEOBJ_BUILDING_WARFACTORY, (warfactory as LoadedBuildingGameObj).factoryChunkId)
    }
}
