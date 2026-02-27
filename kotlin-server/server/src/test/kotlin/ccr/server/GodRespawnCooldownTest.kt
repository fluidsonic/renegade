package ccr.server

import ccr.server.net.Player
import ccr.server.net.SoldierGameObj
import kotlin.test.Test
import kotlin.test.assertTrue

class GodRespawnCooldownTest {

    /**
     * Minimal God subclass that overrides createCommando to track whether it was called.
     */
    private class TrackingGod(server: Network) : God(server) {
        var createCommandoCalled = false
        override fun createCommando(rhostId: Int, playerType: Int): SoldierGameObj? {
            createCommandoCalled = true
            return null
        }
    }

    private fun makeGod(rhostId: Int, team: Int): Pair<TrackingGod, Network> {
        val config = ServerConfig(mapName = "", gamePort = 4848, rconPort = 4849)
        val server = Network(config)
        val god = TrackingGod(server)

        god.state = God.State.MULTIPLAYER
        god.playerInGame.add(rhostId)
        god.playerTeams[rhostId] = team
        // Populate playersByHost so think() can resolve the Player for createCommando(Player)
        god.playersByHost[rhostId] = Player(id = rhostId, name = "TestPlayer$rhostId", team = team, isInGame = true)

        return Pair(god, server)
    }

    @Test
    fun `no cooldown on first spawn (new player)`() {
        val rhostId = 2
        val (god, _) = makeGod(rhostId, 1)
        // No delay — C++ cGod::Think() spawns immediately; respawn delay is client-side only

        god.think(0.016f)  // one frame
        assertTrue(god.createCommandoCalled, "first spawn must not have a cooldown")
    }
}
