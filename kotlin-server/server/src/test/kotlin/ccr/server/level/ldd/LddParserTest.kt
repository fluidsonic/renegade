package ccr.server.level.ldd

import ccr.server.level.ChunkIds
import ccr.server.level.DefinitionRegistry
import ccr.server.mix.ChunkReader
import ccr.server.net.BuildingGameObj
import ccr.server.net.SimpleGameObj
import ccr.server.net.VehicleGameObj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        val factory = GameObjectFactory(DefinitionRegistry())

        val building = factory.load(ChunkIds.GAMEOBJ_BUILDING, emptyObjData)
        assertTrue(building is BuildingGameObj)
        assertEquals(0, building.definitionId)

        // Soldiers are spawned by god, not created by the factory
        val soldier = factory.load(ChunkIds.GAMEOBJ_SOLDIER, emptyObjData)
        assertNull(soldier)

        val vehicle = factory.load(ChunkIds.GAMEOBJ_VEHICLE, emptyObjData)
        assertTrue(vehicle is VehicleGameObj)

        val simple = factory.load(ChunkIds.GAMEOBJ_SIMPLE, emptyObjData)
        assertTrue(simple is SimpleGameObj)

        // Script zones and other unsupported types return null
        val zone = factory.load(ChunkIds.GAMEOBJ_SCRIPTZONE, emptyObjData)
        assertNull(zone)

        // Unknown chunk IDs return null
        val unknown = factory.load(0xDEADu, emptyObjData)
        assertNull(unknown)
    }

    @Test
    fun `GameObjectFactory creates building subtypes`() {
        val emptyObjData = ChunkReader(ByteArray(0))
        val factory = GameObjectFactory(DefinitionRegistry())

        val building = factory.load(ChunkIds.GAMEOBJ_BUILDING, emptyObjData)
        assertTrue(building is BuildingGameObj)

        val refinery = factory.load(ChunkIds.GAMEOBJ_BUILDING_REFINERY, emptyObjData)
        assertTrue(refinery is BuildingGameObj)

        val warfactory = factory.load(ChunkIds.GAMEOBJ_BUILDING_WARFACTORY, emptyObjData)
        assertTrue(warfactory is BuildingGameObj)
    }
}
