package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RefineryGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = RefineryGameObj(
            definitionId = 800,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 20f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = RefineryGameObj(
            definitionId = 801,
            position     = Vector3(0f, 5f, 0f),
            sphereCenter = Vector3(0f, 5f, 0f),
            sphereRadius = 10f,
            isDestroyed  = false,
            isPowerOn    = true,
        )
        assertEquals(801, obj.definitionId)
    }
}
