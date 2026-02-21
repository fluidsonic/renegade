package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.ServerConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GameDataTest {

    private fun defaultConfig() = ServerConfig(ownerName = "Server", gameTitle = "")

    @Test
    fun `exportTier1 writes 421 bits for owner Server`() {
        // ipAddr(32) + owner(16+96=112) + gameTitle(16) + port(32) + currentPlayers(32)
        // + maxPlayers(32) + version(32) + exeCrc(32) + stringsCrc(32) + 5×bool(5)
        // + mapCrc(32) + modCrc(32) = 421 bits
        val gameData = GameData(defaultConfig(), localIp = 0)
        val bs = BitStream()
        gameData.exportTier1(bs)
        assertEquals(421, bs.bitWritePosition)
    }

    @Test
    fun `exportTier2 writes 185 bits with empty motd`() {
        // 4×int(128) + 4×bool(4) + 3×cncBool(3) + motd(16) + 2×cncBool(2) + credits(32) = 185 bits
        val gameData = GameData(defaultConfig(), localIp = 0)
        val bs = BitStream()
        gameData.exportTier2(bs)
        assertEquals(185, bs.bitWritePosition)
    }

    @Test
    fun `field values round-trip through tier1`() {
        val config = ServerConfig(
            ownerName = "Server",
            gameTitle = "",
            gamePort = 4848,
            maxPlayers = 16,
        )
        val gameData = GameData(config, localIp = 0)
        gameData.currentPlayers = 3

        val bs = BitStream()
        gameData.exportTier1(bs)

        // Read back in field order
        val ipAddr = bs.getInt()
        val owner = bs.getWideString()
        val gameTitle = bs.getWideString(permitEmpty = true)
        val port = bs.getInt()
        val currentPlayers = bs.getInt()
        val maxPlayers = bs.getInt()

        assertEquals(0, ipAddr)
        assertEquals("Server", owner)
        assertEquals("", gameTitle)
        assertEquals(4848, port)
        assertEquals(3, currentPlayers)
        assertEquals(16, maxPlayers)
    }

    @Test
    fun `field values round-trip through tier2`() {
        val config = ServerConfig(
            timeLimitMinutes = 30,
            baseDestructionEndsGame = true,
            beaconPlacementEndsGame = false,
            startingCredits = 500,
        )
        val gameData = GameData(config, localIp = 0)

        val bs = BitStream()
        gameData.exportTier2(bs)

        val timeLimitMinutes = bs.getInt()
        val radarMode = bs.getInt()
        val intermissionTimeSeconds = bs.getInt()
        val minQualifyingTimeMinutes = bs.getInt()
        val isFriendlyFirePermitted = bs.getBool()
        val isFreeWeapons = bs.getBool()
        val isClientTrusted = bs.getBool()
        val remixTeams = bs.getBool()
        val canRepairBuildings = bs.getBool()
        val driverIsAlwaysGunner = bs.getBool()
        val spawnWeapons = bs.getBool()
        val motd = bs.getWideString(permitEmpty = true)
        val baseDestructionEndsGame = bs.getBool()
        val beaconPlacementEndsGame = bs.getBool()
        val startingCredits = bs.getInt()

        assertEquals(30, timeLimitMinutes)
        assertEquals(0, radarMode)
        assertEquals(0, intermissionTimeSeconds)
        assertEquals(0, minQualifyingTimeMinutes)
        assertEquals(false, isFriendlyFirePermitted)
        assertEquals(false, isFreeWeapons)
        assertEquals(false, isClientTrusted)
        assertEquals(false, remixTeams)
        assertEquals(true, canRepairBuildings)
        assertEquals(false, driverIsAlwaysGunner)
        assertEquals(false, spawnWeapons)
        assertEquals("", motd)
        assertEquals(true, baseDestructionEndsGame)
        assertEquals(false, beaconPlacementEndsGame)
        assertEquals(500, startingCredits)
    }
}
