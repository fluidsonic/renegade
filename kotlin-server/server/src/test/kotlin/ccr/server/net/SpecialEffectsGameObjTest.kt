package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SpecialEffectsGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = SpecialEffectsGameObj(definitionId = 300, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds`() {
        val obj = SpecialEffectsGameObj(
            definitionId = 301,
            position     = Vector3(5f, 5f, 5f),
            modelName    = "fx_model",
        )
        assertEquals(301, obj.definitionId)
    }
}
