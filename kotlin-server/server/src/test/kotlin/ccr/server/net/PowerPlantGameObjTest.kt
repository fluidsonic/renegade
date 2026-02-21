package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PowerPlantGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = PowerPlantGameObj(
            definitionId = 1100,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 20f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = PowerPlantGameObj(
            definitionId = 1101,
            position     = Vector3(-10f, 0f, 0f),
            sphereCenter = Vector3(-10f, 0f, 0f),
            sphereRadius = 15f,
            isPowerOn    = false,
        )
        assertEquals(1101, obj.definitionId)
    }
}
