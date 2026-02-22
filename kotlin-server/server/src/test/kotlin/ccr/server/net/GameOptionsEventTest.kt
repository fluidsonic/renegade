package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.ServerConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GameOptionsEventTest {

    private fun defaultConfig() = ServerConfig(ownerName = "Server", gameTitle = "")

    @Test
    fun `creation packet is 807 bits for default Server config`() {
        // header(73) + tier1(421) + tier2(185) + event-specific(128) = 807 bits
        val gameData = GameData(defaultConfig(), localIp = 0)
        val event = GameOptionsEvent(gameData)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 100003)
        assertEquals(807, bs.bitWritePosition)
    }

    @Test
    fun `fields round-trip correctly`() {
        val config = ServerConfig(
            ownerName = "Server",
            gameTitle = "",
            gamePort = 4848,
            maxPlayers = 8,
        )
        val gameData = GameData(config, localIp = 0)
        gameData.currentPlayers = 2

        val event = GameOptionsEvent(gameData)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 100003)

        // Read header
        assertEquals(100003, bs.getInt())         // networkId
        assertEquals(0x0F, bs.getByte().toInt() and 0xFF)  // dirtyBits
        assertEquals(false, bs.getBool())          // isDeletePending
        assertEquals(1008, bs.getInt())            // networkClassId

        // Read tier 1
        assertEquals(0, bs.getInt())               // ipAddr
        assertEquals("Server", bs.getWideString()) // owner
        assertEquals("", bs.getWideString(permitEmpty = true))  // gameTitle
        assertEquals(4848, bs.getInt())            // port
        assertEquals(2, bs.getInt())               // currentPlayers
        assertEquals(8, bs.getInt())               // maxPlayers
        bs.getInt()                                 // versionNumber
        bs.getInt()                                 // exeCrc
        bs.getInt()                                 // stringsCrc
        bs.getBool()                                // isDedicated
        bs.getBool()                                // isTeamChangingAllowed
        bs.getBool()                                // isPassworded
        bs.getBool()                                // isLaddered
        bs.getBool()                                // isClanGame
        bs.getInt()                                 // mapNameCrc
        bs.getInt()                                 // modNameCrc

        // Read tier 2
        bs.getInt()                                 // timeLimitMinutes
        bs.getInt()                                 // radarMode
        bs.getInt()                                 // intermissionTimeSeconds
        bs.getInt()                                 // minQualifyingTimeMinutes
        bs.getBool()                                // isFriendlyFirePermitted
        bs.getBool()                                // isFreeWeapons
        bs.getBool()                                // isClientTrusted
        bs.getBool()                                // remixTeams
        bs.getBool()                                // canRepairBuildings
        bs.getBool()                                // driverIsAlwaysGunner
        bs.getBool()                                // spawnWeapons
        bs.getWideString(permitEmpty = true)        // motd
        bs.getBool()                                // baseDestructionEndsGame
        bs.getBool()                                // beaconPlacementEndsGame
        bs.getInt()                                 // startingCredits

        // Read event-specific fields
        val timeRemaining = bs.getFloat()           // TimeRemainingSeconds (float)
        val hostedGameNumber = bs.getInt()
        val modCrc = bs.getInt()
        val mapCrc = bs.getInt()

        assertEquals(0f, timeRemaining)
        assertEquals(0, hostedGameNumber)
        assertEquals(config.modNameCrc, modCrc)
        assertEquals(config.mapNameCrc, mapCrc)
    }

    @Test
    fun `TimeRemainingSeconds is written as float`() {
        val config = defaultConfig()
        val gameData = GameData(config, localIp = 0)
        gameData.timeRemainingSeconds = 300f

        val event = GameOptionsEvent(gameData)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, networkId = 1)

        // Skip header (73 bits): networkId + dirtyBits + isDeletePending + networkClassId
        bs.getInt(); bs.getByte(); bs.getBool(); bs.getInt()

        // Skip tier 1 (421 bits)
        bs.getInt()                                 // ipAddr
        bs.getWideString()                          // owner "Server"
        bs.getWideString(permitEmpty = true)        // gameTitle ""
        bs.getInt()                                 // port
        bs.getInt(); bs.getInt()                    // currentPlayers, maxPlayers
        bs.getInt(); bs.getInt(); bs.getInt()       // version, exeCrc, stringsCrc
        repeat(5) { bs.getBool() }                  // 5 bools
        bs.getInt(); bs.getInt()                    // mapCrc, modCrc

        // Skip tier 2 (185 bits)
        repeat(4) { bs.getInt() }                   // 4 ints
        repeat(7) { bs.getBool() }                  // 4+3 bools
        bs.getWideString(permitEmpty = true)        // motd ""
        repeat(2) { bs.getBool() }                  // 2 cnc bools
        bs.getInt()                                 // startingCredits

        // Now read TimeRemainingSeconds as float
        val timeFloat = bs.getFloat()
        assertEquals(300f, timeFloat)

        // Verify int(300) ≠ float(300f) bit patterns (documents the bug fix in GameDataUpdateEvent)
        val intBits = java.lang.Float.floatToRawIntBits(300f)
        assertNotEquals(300, intBits)
    }
}
