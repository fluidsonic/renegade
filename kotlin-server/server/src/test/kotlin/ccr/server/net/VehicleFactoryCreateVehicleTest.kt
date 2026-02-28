package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.server.GameObjManager
import ccr.server.Network
import ccr.server.ServerConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests that VehicleFactoryGameObj.createVehicle() uses God.createHarvester / God.createVehicle
 * instead of the stub ObjectLibraryManager.createObject().
 *
 * C++ reference: VehicleFactoryGameObj::Create_Vehicle() calls ObjectLibraryManager::Create_Object().
 * The Kotlin server routes this through God (which owns the full vehicle spawn pipeline)
 * via baseController?.network?.god.
 */
class VehicleFactoryCreateVehicleTest {

    /**
     * Subclass that exposes the protected createVehicle() for unit-testing.
     */
    private class TestableVehicleFactory(
        definitionId: Int,
        position: Vector3 = Vector3(),
    ) : VehicleFactoryGameObj(definitionId, position) {
        fun callCreateVehicle(): VehicleGameObj? = createVehicle()
    }

    private fun makeServer(): Network {
        val config = ServerConfig(mapName = "", gamePort = 4848, rconPort = 4849)
        return Network(config)
    }

    @Test
    fun `createVehicle returns null when baseController is not set`() {
        val factory = TestableVehicleFactory(definitionId = 800)
        factory.generatingVehicleID = 101
        factory.creationTm = Matrix3D.translation(Vector3(10f, 20f, 0f))

        // No baseController wired — should return null
        val result = factory.callCreateVehicle()
        assertNull(result, "Expected null when baseController is not set")

        // Cleanup
        GameObjManager.remove(factory)
        GameObjManager.removeBuilding(factory)
    }

    @Test
    fun `createVehicle returns null when generatingVehicleID is 0`() {
        val server = makeServer()
        val controller = BaseControllerClass(playerType = 0)
        controller.network = server

        val factory = TestableVehicleFactory(definitionId = 801)
        factory.generatingVehicleID = 0  // no vehicle to generate
        factory.baseController = controller
        factory.creationTm = Matrix3D.translation(Vector3(10f, 20f, 0f))

        val result = factory.callCreateVehicle()
        assertNull(result, "Expected null when generatingVehicleID == 0")

        // Cleanup
        GameObjManager.remove(factory)
        GameObjManager.removeBuilding(factory)
    }

    @Test
    fun `createVehicle spawns harvester via God when purchaser is null`() {
        val server = makeServer()
        val controller = BaseControllerClass(playerType = 0)
        controller.network = server

        val factory = TestableVehicleFactory(definitionId = 802)
        factory.generatingVehicleID = 101  // non-zero vehicle def ID
        factory.baseController = controller
        factory.creationTm = Matrix3D.translation(Vector3(15f, 25f, 0f))

        // Before: vehicle reference is empty
        assertNull(factory.vehicle.get(), "vehicle reference should be null before createVehicle()")

        val result = factory.callCreateVehicle()

        // createVehicle() should have called god.createHarvester(playerType=0, 101, Vector3(15,25,0))
        // and returned the created VehicleGameObj
        assertNotNull(result, "Expected a VehicleGameObj to be spawned via God.createHarvester()")

        // The factory's vehicle reference should now point to the spawned vehicle
        assertNotNull(factory.vehicle.get(), "factory.vehicle should be set after createVehicle()")

        // Cleanup
        GameObjManager.remove(result)
        GameObjManager.remove(factory)
        GameObjManager.removeBuilding(factory)
    }
}
