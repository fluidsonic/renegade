package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ComCenterGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = ComCenterGameObj(
            definitionId = 1200,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 22f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = ComCenterGameObj(
            definitionId = 1201,
            position     = Vector3(20f, 20f, 0f),
            sphereCenter = Vector3(20f, 20f, 0f),
            sphereRadius = 16f,
        )
        assertEquals(1201, obj.definitionId)
    }
}
