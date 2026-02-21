package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.protocol.Packet
import ccr.net.protocol.PacketType
import ccr.net.protocol.RefusalCode
import ccr.server.NetClassIds

/**
 * Decodes C&C Renegade network packets into human-readable text summaries.
 *
 * Extracted from ProxyDecoderTest so it can be reused by LiveProxy and other tools.
 */
object PacketDecoder {

    /** Network class IDs that represent events (as opposed to game objects). */
    val EVENT_CLASS_IDS = setOf(
        // S->C events (1001-1016)
        1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
        1012, 1013, 1014, 1015, 1016,
        // C->S events (1017-1038)
        1017, 1018, 1019, 1020, 1021, 1022, 1023, 1024, 1025, 1026, 1027,
        1028, 1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1038,
    )

    /**
     * High-level packet decoder. Returns a multi-line string summary of the packet contents.
     *
     * Handles CONNECT_CS, ACCEPT_SC, REFUSAL_SC, KEEPALIVE, RELIABLE/UNRELIABLE game objects, and ACK.
     */
    fun decodePacket(packet: Packet, direction: String): String {
        val summary = StringBuilder()
        summary.append("${packet.type.name} id=${packet.id} sender=${packet.senderId} payload=${packet.bitLength}b")

        when (packet.type) {
            PacketType.CONNECT_CS -> {
                try {
                    val bs = clonePayload(packet)
                    val nickname = bs.getWideString()
                    val password = bs.getWideString(permitEmpty = true)
                    val exeKey = bs.getInt()
                    val bbo = bs.getInt()
                    summary.append("\n      CONNECT: nick='$nickname' pwd='$password' exeKey=0x${Integer.toUnsignedString(exeKey, 16)} bbo=$bbo")
                } catch (e: Exception) {
                    summary.append("\n      CONNECT decode error: ${e.message}")
                }
            }

            PacketType.ACCEPT_SC -> {
                try {
                    val bs = clonePayload(packet)
                    val assignedId = bs.getInt()
                    summary.append("\n      ACCEPT: assignedId=$assignedId")
                } catch (e: Exception) {
                    summary.append("\n      ACCEPT decode error: ${e.message}")
                }
            }

            PacketType.REFUSAL_SC -> {
                try {
                    val bs = clonePayload(packet)
                    val code = bs.getInt()
                    summary.append("\n      REFUSAL: code=$code (${RefusalCode.fromId(code)})")
                } catch (e: Exception) {
                    summary.append("\n      REFUSAL decode error: ${e.message}")
                }
            }

            PacketType.KEEPALIVE -> {
                try {
                    val bs = clonePayload(packet)
                    val packetLoss = bs.getFloat()
                    val serviceRate = bs.getInt()
                    summary.append("\n      KEEPALIVE: loss=${"%.1f".format(packetLoss)}% svcRate=$serviceRate")
                } catch (e: Exception) {
                    summary.append("\n      KEEPALIVE decode error: ${e.message}")
                }
            }

            PacketType.RELIABLE, PacketType.UNRELIABLE -> {
                if (packet.bitLength >= 41) {
                    try {
                        val bs = clonePayload(packet)
                        val objInfo = decodeGameObjectPayload(bs, direction)
                        summary.append("\n$objInfo")
                    } catch (e: Exception) {
                        summary.append("\n      GAME OBJ decode error: ${e.message}")
                    }
                }
            }

            PacketType.ACK -> {
                // ACK packets have no payload — id already shown above
            }

            else -> {
                // Unknown packet types logged with basic header info
            }
        }

        return summary.toString()
    }

    /**
     * Decodes a network object payload from a RELIABLE/UNRELIABLE packet.
     * Wire layout: [networkId:32][dirtyBits:8][isDeletePending:1][classId:32 if BIT_CREATION]
     */
    fun decodeGameObjectPayload(
        bs: BitStream,
        direction: String,
        indent: String = "      ",
    ): String {
        val sb = StringBuilder()

        val networkId = bs.getInt()
        val dirtyBits = bs.getByte().toInt() and 0xFF
        val isDeletePending = bs.getBool()

        sb.append("${indent}netId=$networkId dirty=0x${dirtyBits.toString(16).uppercase().padStart(2, '0')}")
        if (isDeletePending) sb.append(" DELETE_PENDING")

        val isCreation = (dirtyBits and 0x08) != 0
        val isRare = (dirtyBits and 0x04) != 0
        val isOccasional = (dirtyBits and 0x02) != 0
        val isFrequent = (dirtyBits and 0x01) != 0

        if (isCreation) {
            val classId = bs.getInt()
            val className = NetClassIds.name(classId)
            sb.append(" classId=$classId($className)")

            try {
                when (classId) {
                    1010 -> { // TEAM
                        val teamNumber = bs.getInt()
                        sb.append("\n${indent}  TEAM: teamNumber=$teamNumber (${if (teamNumber == 0) "NOD" else "GDI"})")
                        if (isRare) {
                            val kills = bs.getInt()
                            val deaths = bs.getInt()
                            sb.append(" kills=$kills deaths=$deaths")
                        }
                        if (isOccasional) {
                            val score = bs.getFloat()
                            sb.append(" score=$score")
                        }
                    }

                    1008 -> { // GAMEOPTIONSEVENT
                        sb.append("\n${indent}  GAMEOPTIONSEVENT:")
                        val ipAddress = bs.getInt()
                        val ip = "${(ipAddress ushr 24) and 0xFF}.${(ipAddress ushr 16) and 0xFF}.${(ipAddress ushr 8) and 0xFF}.${ipAddress and 0xFF}"
                        sb.append("\n${indent}    tier1: ip=$ip")
                        val owner = bs.getWideString()
                        sb.append(" owner='$owner'")
                        val gameTitle = bs.getWideString(permitEmpty = true)
                        sb.append(" title='$gameTitle'")
                        val port = bs.getInt()
                        sb.append(" port=$port")
                        val currentPlayers = bs.getInt()
                        sb.append(" players=$currentPlayers")
                        val maxPlayers = bs.getInt()
                        sb.append("/$maxPlayers")
                        val versionNumber = bs.getInt()
                        sb.append(" ver=0x${Integer.toUnsignedString(versionNumber, 16)}")
                        val exeCrc = bs.getInt()
                        sb.append(" exeCrc=0x${Integer.toUnsignedString(exeCrc, 16)}")
                        val stringsCrc = bs.getInt()
                        sb.append(" strCrc=0x${Integer.toUnsignedString(stringsCrc, 16)}")
                        val isDedicated = bs.getBool()
                        val isTeamChanging = bs.getBool()
                        val isPassworded = bs.getBool()
                        val isLaddered = bs.getBool()
                        val isClanGame = bs.getBool()
                        sb.append(" ded=$isDedicated teamChg=$isTeamChanging pwd=$isPassworded ladder=$isLaddered clan=$isClanGame")
                        val mapCrc = bs.getInt()
                        val modCrc = bs.getInt()
                        sb.append(" mapCrc=0x${Integer.toUnsignedString(mapCrc, 16)} modCrc=0x${Integer.toUnsignedString(modCrc, 16)}")

                        sb.append("\n${indent}    tier2:")
                        val timeLimit = bs.getInt()
                        val radarMode = bs.getInt()
                        val intermission = bs.getInt()
                        val minQualify = bs.getInt()
                        sb.append(" timeLimit=${timeLimit}min radar=$radarMode intermission=$intermission qualify=$minQualify")
                        val friendlyFire = bs.getBool()
                        val freeWeapons = bs.getBool()
                        val clientTrusted = bs.getBool()
                        val remixTeams = bs.getBool()
                        sb.append(" ff=$friendlyFire freeWep=$freeWeapons trusted=$clientTrusted remix=$remixTeams")
                        val canRepair = bs.getBool()
                        val driverGunner = bs.getBool()
                        val spawnWeapons = bs.getBool()
                        sb.append(" repair=$canRepair driverGunner=$driverGunner spawnWep=$spawnWeapons")
                        val motd = bs.getWideString(permitEmpty = true)
                        sb.append(" motd='$motd'")
                        val baseDestruction = bs.getBool()
                        val beacon = bs.getBool()
                        val startCredits = bs.getInt()
                        sb.append(" baseDest=$baseDestruction beacon=$beacon credits=$startCredits")

                        val timeRemaining = bs.getFloat()
                        val hostedGameNumber = bs.getInt()
                        val modNameCrc2 = bs.getInt()
                        val mapNameCrc2 = bs.getInt()
                        sb.append("\n${indent}    event: timeRemaining=${timeRemaining}s hosted=$hostedGameNumber")
                    }

                    1012 -> { // GAMEDATAUPDATEEVENT
                        val timeRemaining = bs.getInt()
                        val hostedGameNumber = bs.getInt()
                        sb.append("\n${indent}  GAMEDATAUPDATEEVENT: timeRemaining=${timeRemaining}s hosted=$hostedGameNumber")
                    }

                    1011 -> { // PLAYER
                        val name = bs.getWideString(permitEmpty = true)
                        sb.append("\n${indent}  PLAYER: name='$name'")
                        if (isRare) {
                            val id = bs.getInt()
                            val ladderPoints = bs.getInt()
                            val team = bs.getInt()
                            val damageScale = bs.getInt()
                            val ping = bs.getInt()
                            val isInGame = bs.getBool()
                            val invulnerable = bs.getBool()
                            val isActive = bs.getBool()
                            val wolRank = bs.getInt()
                            val numWolGames = bs.getInt()
                            sb.append("\n${indent}    rare: id=$id team=$team(${if (team == 0) "NOD" else "GDI"}) inGame=$isInGame active=$isActive ping=$ping")
                        }
                        if (isOccasional) {
                            val score = bs.getFloat()
                            val money = bs.getFloat()
                            val kills = bs.getInt()
                            val deaths = bs.getInt()
                            sb.append("\n${indent}    occ: score=$score money=$money kills=$kills deaths=$deaths")
                        }
                    }

                    1000 -> { // GAMEOBJ — soldier/vehicle/building
                        decodeSoldierCreation(bs, sb, indent, isRare, isOccasional, isFrequent)
                    }

                    1013 -> { // SCPINGRESPONSEEVENT
                        val pingNumber = bs.getInt()
                        sb.append("\n${indent}  SCPINGRESPONSEEVENT: pingNumber=$pingNumber")
                    }

                    1017 -> { // CLIENTCONTROL
                        val clientId = bs.getInt()
                        sb.append("\n${indent}  CLIENTCONTROL: clientId=$clientId")
                    }

                    1020 -> { // CHANGETEAMEVENT
                        val senderId = bs.getInt()
                        sb.append("\n${indent}  CHANGETEAMEVENT: senderId=$senderId")
                    }

                    1025 -> { // BIOEVENT
                        sb.append("\n${indent}  BIOEVENT")
                    }

                    1026 -> { // LOADINGEVENT
                        val senderId = bs.getInt()
                        val isLoading = bs.getBool()
                        sb.append("\n${indent}  LOADINGEVENT: senderId=$senderId isLoading=$isLoading")
                    }

                    1031 -> { // CLIENTFPS
                        val clientId = bs.getInt()
                        sb.append("\n${indent}  CLIENTFPS: clientId=$clientId")
                    }

                    1032 -> { // CSPINGREQUESTEVENT
                        val senderId = bs.getInt()
                        val pingNumber = bs.getInt()
                        sb.append("\n${indent}  CSPINGREQUESTEVENT: senderId=$senderId pingNumber=$pingNumber")
                    }

                    else -> {
                        sb.append(" (unknown creation type, not decoded)")
                    }
                }
            } catch (e: Exception) {
                sb.append("\n${indent}  DECODE ERROR: ${e.message}")
            }
        } else {
            val tierList = mutableListOf<String>()
            if (isRare) tierList.add("RARE")
            if (isOccasional) tierList.add("OCC")
            if (isFrequent) tierList.add("FREQ")
            sb.append(" tiers=[${tierList.joinToString(",")}]")

            if (dirtyBits == 0x01) {
                try {
                    decodeSoldierFrequent(bs, sb, indent)
                } catch (_: Exception) {
                    // Not all frequent updates are soldiers; silently skip
                }
            }
        }

        return sb.toString()
    }

    /**
     * Decodes a GAMEOBJ (classId=1000) creation packet.
     *
     * Since classId=1000 covers ALL game objects (soldiers, vehicles, buildings, static objects),
     * and each subclass has a different export layout, we decode what we can and handle errors
     * gracefully. The C++ server uses a factory to dispatch to the correct subclass; we don't
     * have that factory here, so we try progressively more specific decoding.
     *
     * All GAMEOBJ share PhysicalGameObj::Export_Creation: defId + position + facing.
     * SmartGameObj adds: controlOwner. SoldierGameObj adds more in rare/occ/freq.
     * Non-Smart objects (buildings, static objects) don't have controlOwner.
     */
    fun decodeSoldierCreation(
        bs: BitStream,
        sb: StringBuilder,
        indent: String,
        isRare: Boolean,
        isOccasional: Boolean,
        isFrequent: Boolean,
    ) {
        val definitionId = bs.getInt()
        sb.append("\n${indent}  GAMEOBJ creation: defId=0x${Integer.toUnsignedString(definitionId, 16)}")
        val posX = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val posY = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val posZ = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        sb.append(" pos=(${"%.1f".format(posX)}, ${"%.1f".format(posY)}, ${"%.1f".format(posZ)})")
        val facing = bs.getFloat()
        sb.append(" facing=${"%.2f".format(facing)}")

        val bitsAfterCreation = bs.bitWritePosition - bs.bitReadPosition

        var isSmart = false
        var controlOwner = 0
        if (bitsAfterCreation >= 32) {
            val savedReadPos = bs.bitReadPosition
            controlOwner = bs.getInt()
            isSmart = controlOwner in 0..128 && bitsAfterCreation >= 200
            if (!isSmart) {
                sb.append(" (static/simple obj, ${bitsAfterCreation + 32}b remaining after creation header)")
                return
            }
            sb.append(" controlOwner=$controlOwner")
        } else {
            sb.append(" (${bitsAfterCreation}b remaining)")
            return
        }

        if (isRare && bs.bitWritePosition - bs.bitReadPosition >= 48) {
            try {
                val modelName = bs.getTerminatedString(permitEmpty = true)
                val animName = bs.getTerminatedString(permitEmpty = true)
                val currFrame = bs.getInt()
                val targetFrame = bs.getInt()
                val animMode = bs.getInt()
                val hostModelId = bs.getInt()
                val hostBone = bs.getInt()
                val playerType = bs.getInt()
                val hudPokable = bs.getBool()
                sb.append("\n${indent}    rare: model='$modelName' anim='$animName' frame=$currFrame/$targetFrame mode=$animMode")
                sb.append(" host=$hostModelId/$hostBone team=$playerType pokable=$hudPokable")

                val rareRemaining = bs.bitWritePosition - bs.bitReadPosition
                if (rareRemaining >= 32) {
                    val soldierDefId = bs.getInt()
                    sb.append(" soldierDefId=0x${Integer.toUnsignedString(soldierDefId, 16)}")
                }
            } catch (e: Exception) {
                sb.append("\n${indent}    rare DECODE ERROR: ${e.message}")
            }
        }

        if (isOccasional && bs.bitWritePosition - bs.bitReadPosition >= 27) {
            try {
                val isDead = bs.getBool()
                val health = bs.getInt(BITPACK_HEALTH)
                val shieldStrength = bs.getInt(BITPACK_SHIELD_STRENGTH)
                val shieldType = bs.getInt(BITPACK_SHIELD_TYPE)
                sb.append("\n${indent}    occ: dead=$isDead hp=$health shield=$shieldStrength type=$shieldType")

                val occRemaining = bs.bitWritePosition - bs.bitReadPosition
                if (occRemaining >= 32) {
                    val weaponCount = bs.getInt()
                    sb.append(" weapons=$weaponCount")
                    for (w in 0 until weaponCount.coerceAtMost(10)) {
                        val wepDefId = bs.getInt()
                        val totalRounds = bs.getInt()
                        sb.append("\n${indent}      weapon[$w]: defId=0x${Integer.toUnsignedString(wepDefId, 16)} rounds=$totalRounds")
                    }
                }
            } catch (e: Exception) {
                sb.append("\n${indent}    occ DECODE ERROR: ${e.message}")
            }
        }

        if (isFrequent && bs.bitWritePosition - bs.bitReadPosition >= 20) {
            try {
                decodeSoldierFrequent(bs, sb, indent)
            } catch (e: Exception) {
                sb.append("\n${indent}    freq DECODE ERROR: ${e.message}")
            }
        }
    }

    /**
     * Decodes the Export_Frequent tier for a SoldierGameObj.
     */
    fun decodeSoldierFrequent(bs: BitStream, sb: StringBuilder, indent: String) {
        val inVehicle = bs.getBool()
        sb.append("\n${indent}    freq: inVehicle=$inVehicle")
        if (!inVehicle) {
            val hasWeapon = bs.getBool()
            sb.append(" hasWeapon=$hasWeapon")
            if (hasWeapon) {
                val wepId = bs.getInt()
                val wepRounds = bs.getInt()
                sb.append(" curWeapon=0x${Integer.toUnsignedString(wepId, 16)}/$wepRounds")
            }
            val fPosX = bs.getFloat(BITPACK_WORLD_POSITION_X)
            val fPosY = bs.getFloat(BITPACK_WORLD_POSITION_Y)
            val fPosZ = bs.getFloat(BITPACK_WORLD_POSITION_Z)
            sb.append(" pos=(${"%.1f".format(fPosX)}, ${"%.1f".format(fPosY)}, ${"%.1f".format(fPosZ)})")
            val humanState = bs.getInt(BITPACK_HUMAN_STATE)
            val humanSubState = bs.getInt(BITPACK_HUMAN_SUB_STATE)
            sb.append(" state=$humanState sub=$humanSubState")

            if (humanState == 3) {
                val velX = bs.getFloat()
                val velY = bs.getFloat()
                val velZ = bs.getFloat()
                sb.append(" vel=(${"%.1f".format(velX)}, ${"%.1f".format(velY)}, ${"%.1f".format(velZ)})")
            }
            if (humanState == 7 || humanState == 9) {
                val animStr = bs.getTerminatedString(permitEmpty = true)
                sb.append(" anim='$animStr'")
            }

            val isSpecialDamage = bs.getBool()
            sb.append(" specialDmg=$isSpecialDamage")
            if (isSpecialDamage) {
                val damageMode = bs.getInt()
                sb.append(" damageMode=$damageMode")
            }
        }

        val onHostBone = bs.getBool()
        sb.append(" onHostBone=$onHostBone")

        val tgtX = bs.getFloat(BITPACK_WORLD_POSITION_X)
        val tgtY = bs.getFloat(BITPACK_WORLD_POSITION_Y)
        val tgtZ = bs.getFloat(BITPACK_WORLD_POSITION_Z)
        sb.append(" tgt=(${"%.1f".format(tgtX)}, ${"%.1f".format(tgtY)}, ${"%.1f".format(tgtZ)})")

        val contBool = bs.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toInt() and 0xFF
        val fwd = bs.getFloat(BITPACK_ANALOG_VALUES)
        val left = bs.getFloat(BITPACK_ANALOG_VALUES)
        val up = bs.getFloat(BITPACK_ANALOG_VALUES)
        val turn = bs.getFloat(BITPACK_ANALOG_VALUES)
        sb.append(" ctrl=[bool=$contBool fwd=${"%.2f".format(fwd)} left=${"%.2f".format(left)} up=${"%.2f".format(up)} turn=${"%.2f".format(turn)}]")
    }

    /**
     * Creates a fresh BitStream copy of a packet's payload for reading,
     * without disturbing the original packet's read position.
     */
    fun clonePayload(packet: Packet): BitStream {
        val bs = BitStream()
        val payloadBytes = (packet.bitLength + 7) / 8
        System.arraycopy(packet.payload.buffer, 0, bs.buffer, 0, payloadBytes)
        bs.setBitWritePosition(packet.bitLength)
        return bs
    }
}
