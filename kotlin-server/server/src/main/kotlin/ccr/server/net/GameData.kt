package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.ServerConfig

// C++: cGameData (gamedata.cpp) — game settings container.
// cGameData::Export_Tier_1_Data (gamedata.cpp:776-803)
// cGameDataCnc::Export_Tier_2_Data (gdcnc.cpp:164-171) which calls
//   cGameData::Export_Tier_2_Data (gamedata.cpp:917-936)
// This server always runs in CnC mode (Is_Cnc() == true).
class GameData(config: ServerConfig, localIp: Int) {

    // Tier 1 fields (static from config)
    // C++ sends the server's actual IP here (cGameData::IpAddress = WOL IP or LAN IP).
    val ipAddress: Int = localIp
    val owner: String = config.ownerName.ifEmpty { "Server" }
    val gameTitle: String = config.gameTitle
    val port: Int = config.gamePort
    val maxPlayers: Int = config.maxPlayers
    val versionNumber: Int = config.versionNumber
    val exeCrc: Int = config.exeCrc
    val stringsCrc: Int = config.stringsCrc
    val isDedicated: Boolean = config.isDedicated
    val isTeamChangingAllowed: Boolean = config.isTeamChangingAllowed
    val isPassworded: Boolean = config.isPassworded
    val isLaddered: Boolean = config.isLaddered
    val isClanGame: Boolean = config.isClanGame
    var mapNameCrc: Int = config.mapNameCrc
    val modNameCrc: Int = config.modNameCrc

    // Tier 2 fields (static from config)
    val timeLimitMinutes: Int = config.timeLimitMinutes
    val radarMode: Int = config.radarMode
    val intermissionTimeSeconds: Int = config.intermissionTimeSeconds
    val minQualifyingTimeMinutes: Int = config.minQualifyingTimeMinutes
    val isFriendlyFirePermitted: Boolean = false
    val isFreeWeapons: Boolean = false
    val isClientTrusted: Boolean = config.isClientTrusted
    val remixTeams: Boolean = config.remixTeams
    // Is_Cnc() = true: extra booleans
    val canRepairBuildings: Boolean = true
    val driverIsAlwaysGunner: Boolean = config.driverIsAlwaysGunner
    val spawnWeapons: Boolean = false
    val motd: String = ""
    val baseDestructionEndsGame: Boolean = config.baseDestructionEndsGame
    val beaconPlacementEndsGame: Boolean = config.beaconPlacementEndsGame
    val startingCredits: Int = config.startingCredits

    // Dynamic fields updated at runtime
    var currentPlayers: Int = 0
    var timeRemainingSeconds: Float = 0f
    var hostedGameNumber: Int = 0

    // C++: cGameData::Export_Tier_1_Data (gamedata.cpp:776-803)
    fun exportTier1(packet: BitStream) {
        packet.addInt(ipAddress)
        packet.addWideString(owner)
        packet.addWideString(gameTitle, permitEmpty = true)
        packet.addInt(port)
        packet.addInt(currentPlayers)
        packet.addInt(maxPlayers)
        packet.addInt(versionNumber)
        packet.addInt(exeCrc)
        packet.addInt(stringsCrc)
        packet.addBool(isDedicated)
        packet.addBool(isTeamChangingAllowed)
        packet.addBool(isPassworded)
        packet.addBool(isLaddered)
        packet.addBool(isClanGame)
        packet.addInt(mapNameCrc)
        packet.addInt(modNameCrc)
    }

    // C++: cGameData::Export_Tier_2_Data (gamedata.cpp:917-936) +
    //      cGameDataCnc::Export_Tier_2_Data (gdcnc.cpp:164-171)
    fun exportTier2(packet: BitStream) {
        packet.addInt(timeLimitMinutes)
        packet.addInt(radarMode)
        packet.addInt(intermissionTimeSeconds)
        packet.addInt(minQualifyingTimeMinutes)
        packet.addBool(isFriendlyFirePermitted)
        packet.addBool(isFreeWeapons)
        packet.addBool(isClientTrusted)
        packet.addBool(remixTeams)
        // Is_Cnc() = true:
        packet.addBool(canRepairBuildings)
        packet.addBool(driverIsAlwaysGunner)
        packet.addBool(spawnWeapons)
        packet.addWideString(motd, permitEmpty = true)
        // cGameDataCnc::Export_Tier_2_Data adds these after parent call:
        packet.addBool(baseDestructionEndsGame)
        packet.addBool(beaconPlacementEndsGame)
        packet.addInt(startingCredits)
    }
}
