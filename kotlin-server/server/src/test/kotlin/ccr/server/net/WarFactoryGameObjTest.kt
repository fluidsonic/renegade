package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WarFactoryGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = WarFactoryGameObj(
            definitionId = 600,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 20f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = WarFactoryGameObj(
            definitionId = 601,
            position     = Vector3(5f, 10f, 0f),
            sphereCenter = Vector3(5f, 10f, 0f),
            sphereRadius = 15f,
        )
        assertEquals(601, obj.definitionId)
    }
}
