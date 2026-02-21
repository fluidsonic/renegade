package ccr.server.defs

import ccr.server.mix.ChunkReader

/**
 * Kotlin representation of CNCModeSettingsDef (Combat/CNCModeSettings.h).
 *
 * C++ defaults: AnnouncementInterval(30), all string/sound IDs = 0,
 *   radioCmds[30] = 0, radioCmdIcons[30] = ""
 */
data class CNCModeSettingsDef(
    val name: String,
    val id: UInt,
    val classId: UInt,
    val announcementInterval: Int = 30,
    // Per-team EVA string IDs (index 0 = Nod, 1 = GDI)
    val nodPowerOfflineId: Int = 0,
    val gdiPowerOfflineId: Int = 0,
    val nodPurchaseCanceledId: Int = 0,
    val gdiPurchaseCanceledId: Int = 0,
    val nodInsufficientFundsId: Int = 0,
    val gdiInsufficientFundsId: Int = 0,
    val nodUnitReadyId: Int = 0,
    val gdiUnitReadyId: Int = 0,
    val nodIonBeaconDeployedId: Int = 0,
    val gdiIonBeaconDeployedId: Int = 0,
    val nodIonBeaconDisarmedId: Int = 0,
    val gdiIonBeaconDisarmedId: Int = 0,
    val nodIonBeaconWarningId: Int = 0,
    val gdiIonBeaconWarningId: Int = 0,
    val nodNukeBeaconDeployedId: Int = 0,
    val gdiNukeBeaconDeployedId: Int = 0,
    val nodNukeBeaconDisarmedId: Int = 0,
    val gdiNukeBeaconDisarmedId: Int = 0,
    val nodNukeBeaconWarningId: Int = 0,
    val gdiNukeBeaconWarningId: Int = 0,
    // Radio commands (30 string DB IDs) and icons (30 texture filenames)
    val radioCmds: List<Int> = List(30) { 0 },
    val radioCmdIcons: List<String> = List(30) { "" },
) {
    companion object {
        /** CLASSID_GLOBAL_SETTINGS_DEF_CNCMODE = 0xF00A */
        const val CLASS_ID: UInt = 61450u
    }
}

// Chunk IDs from CNCModeSettings.cpp enum (line 165)
private const val CHUNKID_CNC_VARIABLES = 803001813u // CHUNKID_PARENT + 1

// Micro-chunk IDs (enum starting at 1)
private const val VARID_ANNOUNCEMENT_INTERVAL = 1
private const val VARID_NOD_POWER_OFFLINE_ID = 2
private const val VARID_GDI_POWER_OFFLINE_ID = 3
private const val VARID_NOD_PURCHASE_CANCELED_ID = 4
private const val VARID_GDI_PURCHASE_CANCELED_ID = 5
private const val VARID_NOD_INSUFFICIENT_FUNDS_ID = 6
private const val VARID_GDI_INSUFFICIENT_FUNDS_ID = 7
private const val VARID_NOD_UNIT_READY_ID = 8
private const val VARID_GDI_UNIT_READY_ID = 9
private const val VARID_RADIO_CMD_01 = 10
// VARID_RADIO_CMD_02..30 = 11..39
private const val VARID_NOD_ION_BEACON_DEPLOYED_ID = 40
private const val VARID_GDI_ION_BEACON_DEPLOYED_ID = 41
private const val VARID_NOD_ION_BEACON_DISARMED_ID = 42
private const val VARID_GDI_ION_BEACON_DISARMED_ID = 43
private const val VARID_NOD_ION_BEACON_WARNING_ID = 44
private const val VARID_GDI_ION_BEACON_WARNING_ID = 45
private const val VARID_NOD_NUKE_BEACON_DEPLOYED_ID = 46
private const val VARID_GDI_NUKE_BEACON_DEPLOYED_ID = 47
private const val VARID_NOD_NUKE_BEACON_DISARMED_ID = 48
private const val VARID_GDI_NUKE_BEACON_DISARMED_ID = 49
private const val VARID_NOD_NUKE_BEACON_WARNING_ID = 50
private const val VARID_GDI_NUKE_BEACON_WARNING_ID = 51
private const val VARID_RADIO_ICON_01 = 52
// VARID_RADIO_ICON_02..30 = 53..81

fun parseCNCModeSettingsDef(
    objDataReader: ChunkReader,
    name: String,
    id: UInt,
    classId: UInt,
): CNCModeSettingsDef? {
    val vars = objDataReader.findChunk(CHUNKID_CNC_VARIABLES)
        ?: return CNCModeSettingsDef(name = name, id = id, classId = classId)

    val radioCmds = MutableList(30) { 0 }
    val radioCmdIcons = MutableList(30) { "" }

    for (i in 0 until 30) {
        vars.mcInt(VARID_RADIO_CMD_01 + i)?.let { radioCmds[i] = it }
        vars.mcString(VARID_RADIO_ICON_01 + i)?.let { radioCmdIcons[i] = it }
    }

    return CNCModeSettingsDef(
        name = name, id = id, classId = classId,
        announcementInterval = vars.mcInt(VARID_ANNOUNCEMENT_INTERVAL) ?: 30,
        nodPowerOfflineId = vars.mcInt(VARID_NOD_POWER_OFFLINE_ID) ?: 0,
        gdiPowerOfflineId = vars.mcInt(VARID_GDI_POWER_OFFLINE_ID) ?: 0,
        nodPurchaseCanceledId = vars.mcInt(VARID_NOD_PURCHASE_CANCELED_ID) ?: 0,
        gdiPurchaseCanceledId = vars.mcInt(VARID_GDI_PURCHASE_CANCELED_ID) ?: 0,
        nodInsufficientFundsId = vars.mcInt(VARID_NOD_INSUFFICIENT_FUNDS_ID) ?: 0,
        gdiInsufficientFundsId = vars.mcInt(VARID_GDI_INSUFFICIENT_FUNDS_ID) ?: 0,
        nodUnitReadyId = vars.mcInt(VARID_NOD_UNIT_READY_ID) ?: 0,
        gdiUnitReadyId = vars.mcInt(VARID_GDI_UNIT_READY_ID) ?: 0,
        nodIonBeaconDeployedId = vars.mcInt(VARID_NOD_ION_BEACON_DEPLOYED_ID) ?: 0,
        gdiIonBeaconDeployedId = vars.mcInt(VARID_GDI_ION_BEACON_DEPLOYED_ID) ?: 0,
        nodIonBeaconDisarmedId = vars.mcInt(VARID_NOD_ION_BEACON_DISARMED_ID) ?: 0,
        gdiIonBeaconDisarmedId = vars.mcInt(VARID_GDI_ION_BEACON_DISARMED_ID) ?: 0,
        nodIonBeaconWarningId = vars.mcInt(VARID_NOD_ION_BEACON_WARNING_ID) ?: 0,
        gdiIonBeaconWarningId = vars.mcInt(VARID_GDI_ION_BEACON_WARNING_ID) ?: 0,
        nodNukeBeaconDeployedId = vars.mcInt(VARID_NOD_NUKE_BEACON_DEPLOYED_ID) ?: 0,
        gdiNukeBeaconDeployedId = vars.mcInt(VARID_GDI_NUKE_BEACON_DEPLOYED_ID) ?: 0,
        nodNukeBeaconDisarmedId = vars.mcInt(VARID_NOD_NUKE_BEACON_DISARMED_ID) ?: 0,
        gdiNukeBeaconDisarmedId = vars.mcInt(VARID_GDI_NUKE_BEACON_DISARMED_ID) ?: 0,
        nodNukeBeaconWarningId = vars.mcInt(VARID_NOD_NUKE_BEACON_WARNING_ID) ?: 0,
        gdiNukeBeaconWarningId = vars.mcInt(VARID_GDI_NUKE_BEACON_WARNING_ID) ?: 0,
        radioCmds = radioCmds,
        radioCmdIcons = radioCmdIcons,
    )
}
