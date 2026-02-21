package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CsDamageEventTest {
    @Test fun `creation round-trip`() {
        val event = CsDamageEvent(
            senderId = 2,
            damagerGoid = 100,
            damageeGoid = 200,
            damage = 35.5f,
            warhead = 3,
        )
        val bs = BitStream()
        event.exportCreation(bs)
        val parsed = CsDamageEvent()
        parsed.importCreation(bs)
        assertEquals(2, parsed.senderId)
        assertEquals(100, parsed.damagerGoid)
        assertEquals(200, parsed.damageeGoid)
        assertEquals(35.5f, parsed.damage)
        assertEquals(3, parsed.warhead)
    }

    @Test fun `networkClassId is 1033`() {
        assertEquals(1033, CsDamageEvent().networkClassId)
    }
}
