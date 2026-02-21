package ccr.server.net

import ccr.math.Vector3
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SAMSiteGameObjTest {

    @Test fun `networkClassId is 1000`() {
        val obj = SAMSiteGameObj(definitionId = 400, position = Vector3(0f, 0f, 0f))
        assertEquals(1000, obj.networkClassId)
    }

    @Test fun `instantiation succeeds with all params`() {
        val obj = SAMSiteGameObj(
            definitionId = 401,
            position     = Vector3(10f, 20f, 0f),
            modelName    = "sam_model",
            team         = 1,
        )
        assertEquals(401, obj.definitionId)
    }
}
