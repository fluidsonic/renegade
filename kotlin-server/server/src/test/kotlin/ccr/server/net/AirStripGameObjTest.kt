package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AirStripGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = AirStripGameObj(
            definitionId = 700,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 25f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = AirStripGameObj(
            definitionId = 701,
            position     = Vector3(10f, 0f, 0f),
            sphereCenter = Vector3(10f, 0f, 0f),
            sphereRadius = 30f,
            isBusy       = false,
        )
        assertEquals(701, obj.definitionId)
    }
}
