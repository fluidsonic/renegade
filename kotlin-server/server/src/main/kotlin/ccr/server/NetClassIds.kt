package ccr.server

// C++: Combat/netclassids.h — network class IDs for game objects and events.
// Used to decode the class ID from the RELIABLE/UNRELIABLE payload header for logging.
object NetClassIds {
    private val names = mapOf(
        // S->C objects (1000-1016)
        1000 to "GAMEOBJ",
        1001 to "SCTEXTOBJ",
        1002 to "PLAYERKILL",
        1003 to "WIN",
        1004 to "PURCHASERESPONSEEVENT",
        1005 to "CONSOLECOMMANDEVENT",
        1006 to "RESETWINSEVENT",
        1007 to "SVRGOODBYEEVENT",
        1008 to "GAMEOPTIONSEVENT",
        1009 to "EVICTIONEVENT",
        1010 to "TEAM",
        1011 to "PLAYER",
        1012 to "GAMEDATAUPDATEEVENT",
        1013 to "SCPINGRESPONSEEVENT",
        1014 to "SCEXPLOSIONEVENT",
        1015 to "SCOBELISKEVENT",
        1016 to "SCANNOUNCEMENT",
        // C->S objects (1017-1038)
        1017 to "CLIENTCONTROL",
        1018 to "CSTEXTOBJ",
        1019 to "SUICIDEEVENT",
        1020 to "CHANGETEAMEVENT",
        1021 to "MONEYEVENT",
        1022 to "WARPEVENT",
        1023 to "PURCHASEREQUESTEVENT",
        1024 to "CLIENTGOODBYEEVENT",
        1025 to "BIOEVENT",
        1026 to "LOADINGEVENT",
        1027 to "GODMODEEVENT",
        1028 to "VIPMODEEVENT",
        1029 to "SCOREEVENT",
        1030 to "CLIENTBBOEVENT",
        1031 to "CLIENTFPS",
        1032 to "CSPINGREQUESTEVENT",
        1033 to "CSDAMAGEEVENT",
        1034 to "REQUESTKILLEVENT",
        1035 to "CSCONSOLECOMMANDEVENT",
        1036 to "CSHINT",
        1037 to "CSANNOUNCEMENT",
        1038 to "DONATEEVENT",
    )

    fun name(classId: Int): String = names[classId] ?: "UNKNOWN_$classId"
}
