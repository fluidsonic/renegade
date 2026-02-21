package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SoldierFactoryGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = SoldierFactoryGameObj(
            definitionId = 1000,
            position     = Vector3(0f, 0f, 0f),
            sphereCenter = Vector3(0f, 0f, 0f),
            sphereRadius = 18f,
        )
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = SoldierFactoryGameObj(
            definitionId = 1001,
            position     = Vector3(3f, 7f, 0f),
            sphereCenter = Vector3(3f, 7f, 0f),
            sphereRadius = 12f,
        )
        assertEquals(1001, obj.definitionId)
    }
}
