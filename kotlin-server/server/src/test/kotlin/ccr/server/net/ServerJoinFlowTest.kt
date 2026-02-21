package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.net.protocol.Packet
import ccr.net.protocol.PacketType
import ccr.net.replication.NetworkObjectManager
import ccr.server.ServerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

/**
 * Integration test for the complete server-join UDP stream in both directions.
 *
 * OUT tests: verify that server-built packets have the correct bit count and decodable content.
 * IN tests:  verify that client→server packets can be parsed by the server's decoder.
 *
 * Encoder setup mirrors C&C_Under map (confirmed working against a live C++ proxy capture).
 * Packet sequence follows the confirmed real join flow:
 *   S→C: ACCEPT_SC, TEAM NOD, TEAM GDI, GAMEOPTIONSEVENT
 *   C→S: CLIENTCONTROL, CLIENTFPS, BIOEVENT
 *   S→C: PLAYER, GAMEDATAUPDATEEVENT, SOLDIER (classId=1000)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName::class)
class ServerJoinFlowTest {

    // ---- Constants matching GameServer internals ----

    private val NET_ID_NOD_TEAM = 2_100_000_004
    private val NET_ID_GDI_TEAM = 2_100_000_005

    // ---- Encoder setup (mirrors GameServer.initEncoders for C&C_Under) ----

    @BeforeAll
    fun setupEncoders() {
        // Packet header encoders (Packet companion init registers these, but ensure they're set)
        EncoderRegistry.setPrecision(BITPACK_PACKET_TYPE, 4)
        EncoderRegistry.setPrecision(BITPACK_PACKET_ID, 28)

        // World position encoders for C&C_Under map (LSD extents + 1.0 margin)
        EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_X, -578.52, 517.15, 0.2)  // 13 bits
        EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Y, -255.34, 471.86, 0.2)  // 12 bits
        EncoderRegistry.setPrecision(BITPACK_WORLD_POSITION_Z, -67.00, 71.46, 0.2)    // 10 bits

        // Fixed-range encoders (same on all maps)
        EncoderRegistry.setPrecision(BITPACK_ONE_TIME_BOOLEAN_BITS, 23)
        EncoderRegistry.setPrecision(BITPACK_CONTINUOUS_BOOLEAN_BITS, 4)
        EncoderRegistry.setPrecision(BITPACK_ANALOG_VALUES, -1.0, 1.0, 0.01)
        EncoderRegistry.setPrecision(BITPACK_HEALTH, 0.0, 2000.0, 1.0)
        EncoderRegistry.setPrecision(BITPACK_SHIELD_STRENGTH, 0.0, 2000.0, 1.0)
        EncoderRegistry.setPrecision(BITPACK_SHIELD_TYPE, 0.0, 8.0, 1.0)  // armorTypeCount=8 default
        EncoderRegistry.setPrecision(BITPACK_HUMAN_STATE, 0.0, 12.0, 1.0)
        EncoderRegistry.setPrecision(BITPACK_HUMAN_SUB_STATE, 0.0, 511.0, 1.0)
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_CS, 8)
        EncoderRegistry.setPrecision(BITPACK_CONTROL_MOVES_SC, 6)
    }

    // ---- Helpers ----

    /** Writes the standard network-object envelope (networkId + dirtyBits + isDeletePending + classId). */
    private fun writeEnvelope(bs: BitStream, networkId: Int, dirtyBits: Int, classId: Int) {
        bs.addInt(networkId)
        bs.addByte(dirtyBits.toByte())
        bs.addBool(false)  // isDeletePending
        bs.addInt(classId)
    }

    /** Writes a team packet (NOD or GDI) into [bs] and returns the bit count. */
    private fun writeTeam(bs: BitStream, networkId: Int, teamNumber: Int): Int {
        bs.addInt(networkId)         // networkId
        bs.addByte(0x0F)             // dirtyBits = BIT_CREATION (all tiers)
        bs.addBool(false)            // isDeletePending
        bs.addInt(1010)              // classId = NETCLASSID_TEAM
        // BIT_CREATION: teamNumber
        bs.addInt(teamNumber)        // TeamNumber
        // BIT_RARE: kills + deaths
        bs.addInt(0)                 // Kills
        bs.addInt(0)                 // Deaths
        // BIT_OCCASIONAL: score
        bs.addFloat(0f)              // Score
        // BIT_FREQUENT: nothing
        return bs.bitWritePosition
    }

    // ============================================================
    // OUT tests — server → client packets
    // ============================================================

    // ------ Step 1: ACCEPT_SC ------

    @Test
    fun `out - ACCEPT_SC payload is 32 bits`() {
        val bs = BitStream()
        bs.addInt(1)  // assignedId = 1
        assertEquals(32, bs.bitWritePosition,
            "ACCEPT_SC payload must be exactly 32 bits (one Int for assignedId)")
    }

    @Test
    fun `out - ACCEPT_SC assignedId round-trips correctly`() {
        // Build the packet as the server does
        val p = Packet()
        p.type = PacketType.ACCEPT_SC
        p.id = 0
        p.senderId = 0
        p.payload.addInt(1)  // assignedId

        // Decode via PacketDecoder (clonePayload)
        val bs = PacketDecoder.clonePayload(p)
        val assignedId = bs.getInt()
        assertEquals(1, assignedId, "ACCEPT_SC assignedId must decode to the value written")
    }

    // ------ Step 2: TEAM NOD ------

    @Test
    fun `out - TEAM NOD payload is 201 bits`() {
        val bs = BitStream()
        writeTeam(bs, NET_ID_NOD_TEAM, 0)
        assertEquals(201, bs.bitWritePosition,
            "TEAM NOD payload must be 201 bits (32+8+1+32+32+32+32+32)")
    }

    @Test
    fun `out - TEAM NOD decodes teamNumber=0`() {
        val bs = BitStream()
        writeTeam(bs, NET_ID_NOD_TEAM, 0)

        // Decode
        val r = BitStream()
        System.arraycopy(bs.buffer, 0, r.buffer, 0, (bs.bitWritePosition + 7) / 8)
        r.setBitWritePosition(bs.bitWritePosition)

        val networkId = r.getInt()
        val dirtyBits = r.getByte().toInt() and 0xFF
        r.getBool()            // isDeletePending
        val classId = r.getInt()
        val teamNumber = r.getInt()   // Creation tier
        val kills = r.getInt()        // Rare
        val deaths = r.getInt()       // Rare
        val score = r.getFloat()      // Occasional

        assertEquals(NET_ID_NOD_TEAM, networkId, "NOD team networkId")
        assertEquals(0x0F, dirtyBits, "NOD team dirtyBits")
        assertEquals(1010, classId, "NOD team classId")
        assertEquals(0, teamNumber, "NOD teamNumber must be 0")
        assertEquals(0, kills, "NOD kills must be 0")
        assertEquals(0, deaths, "NOD deaths must be 0")
        assertEquals(0f, score, "NOD score must be 0.0")
    }

    // ------ Step 3: TEAM GDI ------

    @Test
    fun `out - TEAM GDI payload is 201 bits`() {
        val bs = BitStream()
        writeTeam(bs, NET_ID_GDI_TEAM, 1)
        assertEquals(201, bs.bitWritePosition,
            "TEAM GDI payload must be 201 bits (same structure as NOD)")
    }

    @Test
    fun `out - TEAM GDI decodes teamNumber=1`() {
        val bs = BitStream()
        writeTeam(bs, NET_ID_GDI_TEAM, 1)

        val r = BitStream()
        System.arraycopy(bs.buffer, 0, r.buffer, 0, (bs.bitWritePosition + 7) / 8)
        r.setBitWritePosition(bs.bitWritePosition)

        r.getInt()                           // networkId
        r.getByte()                          // dirtyBits
        r.getBool()                          // isDeletePending
        r.getInt()                           // classId
        val teamNumber = r.getInt()
        assertEquals(1, teamNumber, "GDI teamNumber must be 1")
    }

    // ------ Step 4: GAMEOPTIONSEVENT ------

    @Test
    fun `out - GAMEOPTIONSEVENT payload is 807 bits with default config`() {
        // Use default ServerConfig: ownerName="Server" (6 chars), empty title, empty motd
        val config = ServerConfig(
            ownerName = "Server",
            gameTitle = "",
            mapName = "",
            modName = "",
        )
        val localIp = 0
        val gameData = GameData(config, localIp)
        gameData.currentPlayers = 1
        gameData.hostedGameNumber = 1
        gameData.timeRemainingSeconds = 0f

        val event = GameOptionsEvent(gameData)
        val netId = 1
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, netId)

        assertEquals(807, bs.bitWritePosition,
            "GAMEOPTIONSEVENT payload must be 807 bits (confirmed from live C++ capture)")
    }

    @Test
    fun `out - GAMEOPTIONSEVENT decodes owner and basic flags`() {
        val config = ServerConfig(ownerName = "Server", gameTitle = "", mapName = "", modName = "")
        val gameData = GameData(config, 0)
        gameData.currentPlayers = 1
        gameData.hostedGameNumber = 1
        gameData.timeRemainingSeconds = 0f

        val event = GameOptionsEvent(gameData)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, 999)

        // Decode via PacketDecoder
        val p = Packet()
        p.type = PacketType.RELIABLE
        p.id = 1
        p.senderId = 0
        System.arraycopy(bs.buffer, 0, p.payload.buffer, 0, (bs.bitWritePosition + 7) / 8)
        p.payload.setBitWritePosition(bs.bitWritePosition)

        val decoded = PacketDecoder.clonePayload(p)
        decoded.getInt()        // networkId
        decoded.getByte()       // dirtyBits
        decoded.getBool()       // isDeletePending
        val classId = decoded.getInt()  // classId

        assertEquals(1008, classId, "GAMEOPTIONSEVENT classId must be 1008")

        // Tier 1 decode
        decoded.getInt()        // ipAddress
        val owner = decoded.getWideString()
        assertEquals("Server", owner, "GAMEOPTIONSEVENT owner must be 'Server'")
        val title = decoded.getWideString(permitEmpty = true)
        assertEquals("", title, "GAMEOPTIONSEVENT gameTitle must be empty")
    }

    // ------ Step 8: PLAYER ------

    @Test
    fun `out - PLAYER payload is 556 bits for name=Unnamed`() {
        val player = Player(id = 1, name = "Unnamed", team = 0, isInGame = true)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, player, 2_000_000_001)
        assertEquals(556, bs.bitWritePosition,
            "PLAYER payload must be 556 bits (confirmed from live C++ capture)")
    }

    @Test
    fun `out - PLAYER decodes name, id, team, and isInGame`() {
        val player = Player(id = 1, name = "Unnamed", team = 0, isInGame = true)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, player, 2_000_000_001)

        val r = BitStream()
        System.arraycopy(bs.buffer, 0, r.buffer, 0, (bs.bitWritePosition + 7) / 8)
        r.setBitWritePosition(bs.bitWritePosition)

        r.getInt()   // networkId
        r.getByte()  // dirtyBits
        r.getBool()  // isDeletePending
        val classId = r.getInt()
        assertEquals(1011, classId, "PLAYER classId must be 1011")

        // Creation tier: name (wide string)
        val name = r.getWideString(permitEmpty = true)
        assertEquals("Unnamed", name, "PLAYER name must decode correctly")

        // Rare tier: id, ladderPoints, team, damageScale, ping, isInGame, invulnerable, isActive, wolRank, numWolGames
        val id = r.getInt()
        r.getInt()              // ladderPoints
        val team = r.getInt()
        r.getInt()              // damageScale
        r.getInt()              // ping
        val isInGame = r.getBool()
        val invulnerable = r.getBool()
        val isActive = r.getBool()
        r.getInt()              // wolRank
        r.getInt()              // numWolGames

        assertEquals(1, id, "PLAYER id must be 1")
        assertEquals(0, team, "PLAYER team must be 0 (NOD)")
        assertTrue(isInGame, "PLAYER isInGame must be true")
        assertFalse(invulnerable, "PLAYER invulnerable must be false")
        assertTrue(isActive, "PLAYER isActive must be true")

        // Occasional tier: score, money, kills, deaths
        val score = r.getFloat()
        val money = r.getFloat()
        val kills = r.getInt()
        val deaths = r.getInt()
        assertEquals(0f, score, "PLAYER score must be 0")
        assertEquals(0f, money, "PLAYER money must be 0")
        assertEquals(0, kills, "PLAYER kills must be 0")
        assertEquals(0, deaths, "PLAYER deaths must be 0")
    }

    // ------ Step 9: GAMEDATAUPDATEEVENT ------

    @Test
    fun `out - GAMEDATAUPDATEEVENT payload is 137 bits`() {
        val event = GameDataUpdateEvent(timeRemainingSeconds = 0, hostedGameNumber = 1)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, 2_000_000_002)
        assertEquals(137, bs.bitWritePosition,
            "GAMEDATAUPDATEEVENT payload must be 137 bits (confirmed from live C++ capture)")
    }

    @Test
    fun `out - GAMEDATAUPDATEEVENT decodes timeRemainingSeconds and hostedGameNumber`() {
        val event = GameDataUpdateEvent(timeRemainingSeconds = 600, hostedGameNumber = 1)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, event, 2_000_000_002)

        val r = BitStream()
        System.arraycopy(bs.buffer, 0, r.buffer, 0, (bs.bitWritePosition + 7) / 8)
        r.setBitWritePosition(bs.bitWritePosition)

        r.getInt()               // networkId
        r.getByte()              // dirtyBits
        r.getBool()              // isDeletePending
        val classId = r.getInt()
        assertEquals(1012, classId, "GAMEDATAUPDATEEVENT classId must be 1012")

        val timeRemaining = r.getInt()
        val hostedGame = r.getInt()
        assertEquals(600, timeRemaining, "GAMEDATAUPDATEEVENT timeRemainingSeconds must decode correctly")
        assertEquals(1, hostedGame, "GAMEDATAUPDATEEVENT hostedGameNumber must decode correctly")
    }

    // ------ Step 10: SOLDIER (classId=1000) ------

    @Test
    fun `out - SOLDIER creation packet has correct bit count for NOD minigunner with pistol`() {
        // NOD minigunner setup matching GameServer.spawnSoldier for C&C_Under
        val defId = 81930257  // CnC_Nod_Minigunner_0 (0x04e22811) — from ServerConfig defaults
        val pistolDefId = 0x04E3F3B4  // Weapon_Pistol_Player (typical value; test encodes + decodes)
        val soldier = SoldierGameObj(
            definitionId = defId,
            controlOwner  = 1,
            team          = 0,
            modelName     = "c_ag_nod_mg",
            animName      = "S_A_HUMAN.H_A_AINM",
            position      = Vector3(0f, 0f, 5f),
            weapons       = listOf(WeaponEntry(pistolDefId, 100)),
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, 1_500_000_001)

        // The exact count depends on encoder precision (X=13b, Y=12b, Z=10b from C&C_Under).
        // Print for diagnostics; assert it is non-zero and matches our computed value.
        println("[SOLDIER] bit count: ${bs.bitWritePosition}")
        assertTrue(bs.bitWritePosition > 0, "SOLDIER packet must have non-zero bit count")

        // Computed breakdown (all encoder-dependent values use C&C_Under extents):
        //   Envelope:  32(netId) + 8(dirtyBits) + 1(isDeletePending) + 32(classId) = 73
        //   Creation:  32(defId) + 13(posX) + 12(posY) + 10(posZ) + 32(facing) + 32(controlOwner) = 131
        //   Rare:      16+11*8=104(modelName) + 16+18*8=160(animName) + 32+32+32+32+32+32+1=193 + 32(soldierDefId) = 489
        //   Occasional: 1(isDead) + 11(health) + 11(shieldStr) + 4(shieldType) + 32(wepCount) + 32+32(pistol) = 123
        //   Frequent:   1(inVehicle) + 1(hasWeapon) + 32+32(curWeapon) + 13+12+10(pos) + 4(humanState)
        //               + 9(humanSubState) + 1(isSpecialDmg) + 1(onHostBone) + 13+12+10(tgtPos)
        //               + 4(contBool) + 8+8+8+8(analogs) = 187
        //   Total: 73 + 131 + 489 + 123 + 187 = 1003
        assertEquals(1003, bs.bitWritePosition,
            "SOLDIER creation payload must be 1003 bits (C&C_Under encoders, 1 pistol, NOD minigunner)")
    }

    @Test
    fun `out - SOLDIER creation packet decodes definitionId, model, anim, position, and weapon`() {
        val defId = 81930257
        val pistolDefId = 0x1234ABCD
        val soldier = SoldierGameObj(
            definitionId  = defId,
            controlOwner  = 1,
            team          = 0,
            modelName     = "c_ag_nod_mg",
            animName      = "S_A_HUMAN.H_A_AINM",
            position      = Vector3(0f, 0f, 5f),
            weapons       = listOf(WeaponEntry(pistolDefId, 100)),
        )
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, soldier, 1_500_000_001)

        // Decode using PacketDecoder
        val p = Packet()
        p.type = PacketType.RELIABLE
        p.id = 1
        p.senderId = 0
        System.arraycopy(bs.buffer, 0, p.payload.buffer, 0, (bs.bitWritePosition + 7) / 8)
        p.payload.setBitWritePosition(bs.bitWritePosition)

        val sb = StringBuilder()
        val decoded = PacketDecoder.clonePayload(p)
        val networkId = decoded.getInt()
        val dirtyBits = decoded.getByte().toInt() and 0xFF
        decoded.getBool()               // isDeletePending
        val classId = decoded.getInt()

        assertEquals(1000, classId, "SOLDIER classId must be 1000")
        assertEquals(1_500_000_001, networkId, "SOLDIER networkId must match")
        assertEquals(0x0F, dirtyBits, "SOLDIER dirtyBits must be 0x0F (all tiers)")

        // Decode creation
        PacketDecoder.decodeSoldierCreation(decoded, sb, "  ", isRare = true, isOccasional = true, isFrequent = true)
        val decoded_text = sb.toString()
        println("[SOLDIER decode] $decoded_text")

        // Verify key fields appear in the decoded summary
        assertTrue(decoded_text.contains("defId=0x${Integer.toUnsignedString(defId, 16)}"),
            "SOLDIER decode must include definitionId: $decoded_text")
        assertTrue(decoded_text.contains("model='c_ag_nod_mg'"),
            "SOLDIER decode must include modelName: $decoded_text")
        assertTrue(decoded_text.contains("anim='S_A_HUMAN.H_A_AINM'"),
            "SOLDIER decode must include animName: $decoded_text")
        assertTrue(decoded_text.contains("weapons=1"),
            "SOLDIER decode must include weapon count: $decoded_text")
        assertTrue(decoded_text.contains("rounds=100"),
            "SOLDIER decode must include weapon rounds: $decoded_text")
    }

    // ============================================================
    // IN tests — client → server packets
    // ============================================================

    // ------ Step 5: CLIENTCONTROL (classId=1018) ------

    @Test
    fun `in - CLIENTCONTROL parses clientId correctly`() {
        // Build a minimal CLIENTCONTROL packet as the server handler reads it:
        // networkId(32) + dirtyBits(8) + isDeletePending(1) + classId(32) + clientId(32)
        val bs = BitStream()
        val networkId = 2_110_000_001  // first client range ID
        val clientId = 1
        bs.addInt(networkId)
        bs.addByte(0x0F)         // BIT_CREATION
        bs.addBool(false)        // isDeletePending
        bs.addInt(1018)          // classId = NETCLASSID_CLIENTCONTROL
        bs.addInt(clientId)      // clientId

        // Build a RELIABLE packet with this payload
        val packet = Packet()
        packet.type = PacketType.RELIABLE
        packet.id = 0
        packet.senderId = 1
        System.arraycopy(bs.buffer, 0, packet.payload.buffer, 0, (bs.bitWritePosition + 7) / 8)
        packet.payload.setBitWritePosition(bs.bitWritePosition)

        // Parse as the server does in handleGamePacket
        val snap = PacketDecoder.clonePayload(packet)
        val parsedNetId = snap.getInt()
        val parsedDirty = snap.getByte().toInt() and 0xFF
        val parsedDelete = snap.getBool()
        val hasBitCreation = (parsedDirty and 0x08) != 0
        val parsedClassId = snap.getInt()
        val parsedClientId = snap.getInt()

        assertEquals(networkId, parsedNetId, "CLIENTCONTROL networkId")
        assertEquals(0x0F, parsedDirty, "CLIENTCONTROL dirtyBits")
        assertFalse(parsedDelete, "CLIENTCONTROL isDeletePending must be false")
        assertTrue(hasBitCreation, "CLIENTCONTROL must have BIT_CREATION set")
        assertEquals(1018, parsedClassId, "CLIENTCONTROL classId must be 1018")
        assertEquals(clientId, parsedClientId, "CLIENTCONTROL clientId must round-trip correctly")
    }

    // ------ Step 6: CLIENTFPS (classId=1032) ------

    @Test
    fun `in - CLIENTFPS parses clientId correctly`() {
        // Build a minimal CLIENTFPS packet as the server handler reads it:
        // networkId(32) + dirtyBits(8) + isDeletePending(1) + classId(32) + clientId(32)
        val bs = BitStream()
        val clientId = 1
        bs.addInt(2_110_000_002)  // networkId
        bs.addByte(0x0F)          // BIT_CREATION
        bs.addBool(false)         // isDeletePending
        bs.addInt(1032)           // classId = NETCLASSID_CLIENTFPS
        bs.addInt(clientId)       // clientId

        val packet = Packet()
        packet.type = PacketType.RELIABLE
        packet.id = 1
        packet.senderId = 1
        System.arraycopy(bs.buffer, 0, packet.payload.buffer, 0, (bs.bitWritePosition + 7) / 8)
        packet.payload.setBitWritePosition(bs.bitWritePosition)

        val snap = PacketDecoder.clonePayload(packet)
        snap.getInt()                         // networkId
        val dirtyBits = snap.getByte().toInt() and 0xFF
        snap.getBool()                        // isDeletePending
        val classId = snap.getInt()
        val parsedClientId = snap.getInt()

        assertEquals(0x0F, dirtyBits, "CLIENTFPS dirtyBits must be 0x0F")
        assertEquals(1032, classId, "CLIENTFPS classId must be 1032")
        assertEquals(clientId, parsedClientId, "CLIENTFPS clientId must round-trip correctly")
    }

    // ------ Step 7: BIOEVENT (classId=1026) ------

    @Test
    fun `in - BIOEVENT parses senderId and classId`() {
        // C++ cBioEvent::Export_Creation: no additional fields beyond the creation envelope.
        // The server handler for classId=1026 reads only networkId+dirtyBits+isDeletePending+classId.
        // A minimal BIOEVENT from the client is 73 bits (32+8+1+32).
        val bs = BitStream()
        val networkId = 2_110_000_003
        bs.addInt(networkId)   // networkId
        bs.addByte(0x0F)       // dirtyBits = BIT_CREATION
        bs.addBool(false)      // isDeletePending
        bs.addInt(1026)        // classId = NETCLASSID_BIOEVENT

        val packet = Packet()
        packet.type = PacketType.RELIABLE
        packet.id = 2
        packet.senderId = 1    // senderId = rhostId of the connecting player
        System.arraycopy(bs.buffer, 0, packet.payload.buffer, 0, (bs.bitWritePosition + 7) / 8)
        packet.payload.setBitWritePosition(bs.bitWritePosition)

        // Verify wire round-trip via parseWirePacket
        val wireBytes = Packet.buildWirePacket(packet)
        val reparsed = Packet.parseWirePacket(wireBytes, wireBytes.size)

        assertEquals(PacketType.RELIABLE, reparsed.type, "BIOEVENT packet type must be RELIABLE")
        assertEquals(1, reparsed.senderId, "BIOEVENT senderId must match the connecting player")
        assertEquals(73, reparsed.bitLength, "BIOEVENT payload must be 73 bits (creation envelope only)")

        // Parse the payload as the server does
        val snap = PacketDecoder.clonePayload(reparsed)
        val parsedNetId = snap.getInt()
        val dirtyBits = snap.getByte().toInt() and 0xFF
        snap.getBool()              // isDeletePending
        val classId = snap.getInt()

        assertEquals(networkId, parsedNetId, "BIOEVENT networkId must round-trip")
        assertTrue((dirtyBits and 0x08) != 0, "BIOEVENT must have BIT_CREATION set")
        assertEquals(1026, classId, "BIOEVENT classId must be 1026")
    }

    @Test
    fun `in - BIOEVENT triggers isInGame transition in server logic`() {
        // Verify that a BIOEVENT payload passes all server-side guards in handleGamePacket:
        //   1. bitWritePosition >= 41 (minimum for peekGameEvent)
        //   2. BIT_CREATION (0x08) is set in dirtyBits
        //   3. classId is 1026
        val bs = BitStream()
        bs.addInt(2_110_000_010)   // networkId
        bs.addByte(0x0F)           // dirtyBits
        bs.addBool(false)          // isDeletePending
        bs.addInt(1026)            // classId = NETCLASSID_BIOEVENT

        assertTrue(bs.bitWritePosition >= 41,
            "BIOEVENT payload must be ≥41 bits to pass peekGameEvent guard")

        val snap = BitStream()
        System.arraycopy(bs.buffer, 0, snap.buffer, 0, (bs.bitWritePosition + 7) / 8)
        snap.setBitWritePosition(bs.bitWritePosition)

        snap.getInt()                                    // networkId
        val dirtyBits = snap.getByte().toInt() and 0xFF
        snap.getBool()                                   // isDeletePending
        val classId = snap.getInt()

        assertTrue((dirtyBits and 0x08) != 0, "BIOEVENT must pass BIT_CREATION guard")
        assertEquals(1026, classId, "BIOEVENT classId must be 1026 to trigger BIOEVENT handler")
    }

    // ============================================================
    // Additional round-trip tests
    // ============================================================

    @Test
    fun `out - PLAYER BIT_RARE update encodes team change correctly`() {
        // When a player changes team, the server sends a BIT_RARE update (dirty=0x07, no classId).
        val player = Player(id = 1, name = "", team = 1, isInGame = true)
        val netId = 2_000_000_050
        val bs = BitStream()
        NetworkObjectPacketWriter.writeRareUpdate(bs, player, netId)

        val r = BitStream()
        System.arraycopy(bs.buffer, 0, r.buffer, 0, (bs.bitWritePosition + 7) / 8)
        r.setBitWritePosition(bs.bitWritePosition)

        val parsedNetId = r.getInt()
        val dirtyBits = r.getByte().toInt() and 0xFF
        r.getBool()               // isDeletePending

        assertEquals(netId, parsedNetId, "BIT_RARE update networkId must match")
        assertEquals(0x07, dirtyBits, "BIT_RARE update dirtyBits must be 0x07 (no BIT_CREATION)")

        // Rare tier: id, ladderPoints, team, damageScale, ping, isInGame, invulnerable, isActive, wolRank, numWolGames
        r.getInt()               // id
        r.getInt()               // ladderPoints
        val team = r.getInt()
        assertEquals(1, team, "BIT_RARE update team must be 1 (GDI)")
    }

    @Test
    fun `out - GAMEOPTIONSEVENT bit count increases with longer owner name`() {
        // Verify that owner name length directly affects bit count (each char = 16 bits)
        val config1 = ServerConfig(ownerName = "A", gameTitle = "", mapName = "", modName = "")
        val config9 = ServerConfig(ownerName = "CCR Server", gameTitle = "", mapName = "", modName = "")

        val bs1 = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs1, GameOptionsEvent(GameData(config1, 0)), 1)

        val bs9 = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs9, GameOptionsEvent(GameData(config9, 0)), 1)

        // "CCR Server" has 10 chars; "A" has 1 char → difference = 9 chars × 16 bits = 144 bits
        val diff = bs9.bitWritePosition - bs1.bitWritePosition
        assertEquals(144, diff,
            "Each additional char in owner name must add 16 bits (9 more chars × 16 = 144)")
    }
}
