package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PowerUpGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = PowerUpGameObj(definitionId = 100, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = PowerUpGameObj(
            definitionId   = 200,
            position       = Vector3(1f, 2f, 3f),
            modelName      = "somemodel",
            health         = 50f,
        )
        assertEquals(200, obj.definitionId)
    }
}
