package ccr.net.flow

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlowControllerTest {

    @Test
    fun `shouldSend returns true for all priorities initially`() {
        val fc = FlowController()
        assertTrue(fc.shouldSend(50.0f), "priority 50 should pass fresh controller")
        assertTrue(fc.shouldSend(0.0f), "priority 0 should pass fresh controller")
        assertTrue(fc.shouldSend(99.9f), "priority 99.9 should pass fresh controller")
    }

    @Test
    fun `shouldSend returns false for negative priority`() {
        val fc = FlowController()
        assertFalse(fc.shouldSend(-1.0f), "negative priority should not pass threshold 0")
    }

    @Test
    fun `threshold rises under sustained overrun and blocks low-priority objects`() {
        val fc = FlowController()
        // TP_INCREMENT = 0.01, so 600 overrun ticks raise threshold by 6.0
        repeat(600) { fc.adjust(targetBps = 1000, actualBpsSent = 5000, sampleTimeMs = 50f) }
        assertFalse(fc.shouldSend(0.0f), "after sustained overrun, priority-0 should be blocked")
    }

    @Test
    fun `adjust with overrun lowers bandwidth multiplier`() {
        val fc = FlowController()
        fc.adjust(targetBps = 1000, actualBpsSent = 100_000, sampleTimeMs = 50f)
        assertTrue(fc.bandwidthMultiplier < 1.0f)
    }
}
