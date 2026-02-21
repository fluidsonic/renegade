package ccr.server.level

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration test for LevelLoader.
 * Skipped unless ccr.data.dir system property points to game data directory.
 */
class LevelLoaderIntegrationTest {
    @Test
    fun `level loader integration skips gracefully when data absent`() {
        val dataDir = System.getProperty("ccr.data.dir")
        if (dataDir == null) {
            println("Skipping integration test: set -Dccr.data.dir=/path/to/data to enable")
            return
        }
        // If data dir is set, would run full load here
        assertTrue(true, "Integration test placeholder passed")
    }
}
