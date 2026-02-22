package ccr.server

import ccr.server.net.Player
import ccr.server.net.SoldierGameObj
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GodRespawnCooldownTest {

    /**
     * Minimal God subclass that overrides createCommando to track whether it was called.
     */
    private class TrackingGod(server: GameServer) : God(server) {
        var createCommandoCalled = false
        override fun createCommando(rhostId: Int, playerType: Int): SoldierGameObj? {
            createCommandoCalled = true
            return null
        }
    }

    private fun makeGod(rhostId: Int, team: Int): Pair<TrackingGod, GameServer> {
        val config = ServerConfig(mapName = "", gamePort = 4848, rconPort = 4849)
        val server = GameServer(config)
        val god = TrackingGod(server)

        god.state = God.State.MULTIPLAYER
        god.playerInGame.add(rhostId)
        god.playerTeams[rhostId] = team
        // Populate playersByHost so think() can resolve the Player for createCommando(Player)
        god.playersByHost[rhostId] = Player(id = rhostId, name = "TestPlayer$rhostId", team = team, isInGame = true)

        return Pair(god, server)
    }

    @Test
    fun `respawn is suppressed during cooldown period`() {
        val rhostId = 1
        val (god, _) = makeGod(rhostId, 0)

        // Start cooldown — simulates deleteSoldier starting the timer
        god.startRespawnCooldown(rhostId)

        // Tick with 1.0s — timer is 3s, so createCommando must NOT be called
        god.think(1.0f)
        assertFalse(god.createCommandoCalled, "createCommando must not be called during cooldown (1s elapsed)")

        // Tick another 1.5s (total 2.5s) — still in cooldown
        god.think(1.5f)
        assertFalse(god.createCommandoCalled, "createCommando must not be called during cooldown (2.5s elapsed)")

        // Tick another 1.0s (total 3.5s) — cooldown expired, createCommando should be called
        god.think(1.0f)
        assertTrue(god.createCommandoCalled, "createCommando must be called after 3+ seconds")
    }

    @Test
    fun `no cooldown on first spawn (new player)`() {
        val rhostId = 2
        val (god, _) = makeGod(rhostId, 1)
        // No startRespawnCooldown called — first spawn has no cooldown

        god.think(0.016f)  // one frame
        assertTrue(god.createCommandoCalled, "first spawn must not have a cooldown")
    }
}
