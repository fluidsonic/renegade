package ccr.server

private const val SECTION = "Settings"

/**
 * C++: CRC_Stringi in wwlib/realcrc.cpp — case-insensitive CRC32 over ASCII string bytes.
 * Equivalent to java.util.zip.CRC32 over the uppercase bytes of the string.
 * Known value: crcStringi("C&C_Under.mix") == 721292856 (0x2B007C38).
 */
fun crcStringi(s: String): Int {
    val crc = java.util.zip.CRC32()
    for (c in s) crc.update(c.uppercaseChar().code and 0xFF)
    return crc.value.toInt()
}

/**
 * Immutable server configuration loaded from an INI file's [Settings] section.
 */
data class ServerConfig(
    val gamePort: Int = 4848,
    val maxPlayers: Int = 16,
    val serverName: String = "CCR Server",
    val ownerName: String = "Server",
    val gameTitle: String = "",
    val mapName: String = "",
    val modName: String = "",
    val password: String = "",
    val isDedicated: Boolean = false,
    val isPassworded: Boolean = false,
    val isTeamChangingAllowed: Boolean = true,
    val isLaddered: Boolean = false,
    val isClanGame: Boolean = false,
    val timeLimitMinutes: Int = 0,
    val baseDestructionEndsGame: Boolean = true,
    val beaconPlacementEndsGame: Boolean = true,
    val startingCredits: Int = 0,
    // ExeKey = CRC32("RENEGADE {build} strings.tdb {stringsVersion} ") XOR data_file_crc.
    // Set in gamedata.cpp via Set_Version_Number(cNetwork::Get_Exe_Key()).
    // Sent as VersionNumber in Export_Tier_1_Data and validated against the client's connect key.
    // 0 = skip version check. Default is vanilla Renegade 1.037 (build 838, strings v145).
    val versionNumber: Int = -1084188140,  // 0xbf609a14
    // ExeCRC = CRC32("RENEGADE {build}"). Broadcast-only; not used for connection validation.
    val exeCrc: Int = -1960760632,         // 0x8b212ac8
    // StringsCRC = CRC32("strings.tdb {stringsVersion}"). Broadcast-only.
    val stringsCrc: Int = 1218164589,      // 0x489bb76d
    val rconPort: Int = 63999,
    val rconPassword: String = "",
    val remoteAdminAllowed: Boolean = false,
    val netUpdateRate: Int = 8,
    val bandwidthBps: Int = 0,
    // Path to the directory containing game data files (e.g. C&C_Under.mix).
    // Relative to the working directory, or absolute. Defaults to "data/" subdirectory.
    val dataPath: String = "data",
    // Number of armor types defined in the game's armor.ini (always.dat → always.mix).
    // Used for BITPACK_SHIELD_TYPE encoder precision. Vanilla Renegade multiplayer uses 8.
    // Must match the client's armor type count exactly or shield-type bits will desync.
    val armorTypeCount: Int = 8,
    // Definition IDs for soldier presets (extracted from always.dbs, micro-chunk ID field).
    // CnC_Nod_Minigunner_0 = 0x04e22811, CnC_GDI_MiniGunner_0 = 0x04e22803.
    val nodSoldierDefId: Int = 81930257,   // 0x04e22811  CnC_Nod_Minigunner_0
    val gdiSoldierDefId: Int = 81930243,   // 0x04e22803  CnC_GDI_MiniGunner_0
    val intermissionTimeSeconds: Int = 30,
    val mapList: List<String> = emptyList(),
    val mapCycleLoops: Boolean = false,
) {
    // C++: gamedata.cpp Export_Tier_1_Data calls CRC_Stringi(MapName) / CRC_Stringi(ModName).
    // Empty name → CRC is 0 (loop never executes → crc = 0 ^ 0xFFFFFFFF ^ 0xFFFFFFFF = 0).
    val mapNameCrc: Int get() = if (mapName.isEmpty()) 0 else crcStringi(mapName)
    val modNameCrc: Int get() = if (modName.isEmpty()) 0 else crcStringi(modName)
    /**
     * Effective map list for rotation: uses mapList if non-empty, otherwise falls back to
     * a single-item list containing mapName (or empty if mapName is also empty).
     */
    val effectiveMapList: List<String> get() = when {
        mapList.isNotEmpty() -> mapList
        mapName.isNotEmpty() -> listOf(mapName)
        else -> emptyList()
    }
    companion object {
        fun from(ini: IniParser): ServerConfig = ServerConfig(
            gamePort            = ini.getInt(SECTION, "GamePort", 4848),
            maxPlayers          = ini.getInt(SECTION, "MaxPlayers", 16),
            serverName          = ini.getString(SECTION, "ServerName"),
            ownerName           = ini.getString(SECTION, "OwnerName"),
            gameTitle           = ini.getString(SECTION, "GameTitle"),
            mapName             = ini.getString(SECTION, "MapName"),
            modName             = ini.getString(SECTION, "ModName"),
            password            = ini.getString(SECTION, "Password"),
            isDedicated         = ini.getBool(SECTION, "IsDedicated"),
            isPassworded        = ini.getBool(SECTION, "IsPassworded"),
            isTeamChangingAllowed = ini.getBool(SECTION, "IsTeamChangingAllowed", true),
            isLaddered          = ini.getBool(SECTION, "IsLaddered"),
            isClanGame          = ini.getBool(SECTION, "IsClanGame"),
            timeLimitMinutes    = ini.getInt(SECTION, "TimeLimitMinutes"),
            baseDestructionEndsGame = ini.getBool(SECTION, "BaseDestructionEndsGame", true),
            beaconPlacementEndsGame = ini.getBool(SECTION, "BeaconPlacementEndsGame", true),
            startingCredits     = ini.getInt(SECTION, "StartingCredits"),
            versionNumber       = ini.getInt(SECTION, "VersionNumber", -1084188140),
            exeCrc              = ini.getInt(SECTION, "ExeCRC", -1960760632),
            stringsCrc          = ini.getInt(SECTION, "StringsCRC", 1218164589),
            rconPort            = ini.getInt(SECTION, "RconPort", 63999),
            rconPassword        = ini.getString(SECTION, "RconPassword"),
            remoteAdminAllowed  = ini.getBool(SECTION, "RemoteAdminAllowed"),
            netUpdateRate       = ini.getInt(SECTION, "NetUpdateRate", 8),
            bandwidthBps        = ini.getInt(SECTION, "BandwidthBps"),
            dataPath            = ini.getString(SECTION, "DataPath"),
            armorTypeCount      = ini.getInt(SECTION, "ArmorTypeCount", 8),
            nodSoldierDefId     = ini.getInt(SECTION, "NodSoldierDefId", 81930257),
            gdiSoldierDefId     = ini.getInt(SECTION, "GdiSoldierDefId", 81930243),
            intermissionTimeSeconds = ini.getInt(SECTION, "IntermissionTimeSeconds", 30),
            mapCycleLoops = ini.getBool(SECTION, "MapCycleLoops"),
            mapList = buildList {
                for (i in 1..32) {
                    val v = ini.getString(SECTION, "Map$i")
                    if (v.isNotEmpty()) add(v) else break
                }
            },
        )
    }
}
