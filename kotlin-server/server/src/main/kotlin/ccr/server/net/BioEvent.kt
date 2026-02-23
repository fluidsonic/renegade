package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cBioEvent (Commando/bioevent.h) — networkClassId = NETCLASSID_BIOEVENT = 1025
// C→S event object for transmitting initial player bio data on join.
// The server imports (reads) this; the client exports (writes) it.
//
// Wire format (Export_Creation):
//   Add(SenderId: int)
//   Add_Wide_Terminated_String(Nickname)    — UTF-16LE, length-prefixed (USHORT + chars as USHORT)
//   Add(TeamChoice: int)                    — 0=NOD, 1=GDI, -1=random
//   Add(ClanID: unsigned int)               — stored as int (same 32 bits)
//   Add_Terminated_String(MapName, false)   — ASCII, length-prefixed (USHORT + chars as BYTE)
class BioEvent(
    var senderId: Int = 0,
    var nickname: String = "",
    var teamChoice: Int = 0,
    var clanId: Int = 0,
    var mapName: String = "",
) : NetEvent() {
    override val networkClassId: Int = 1025

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addWideString(nickname)
        packet.addInt(teamChoice)
        packet.addInt(clanId)
        packet.addTerminatedString(mapName)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        nickname = packet.getWideString(permitEmpty = true)
        teamChoice = packet.getInt()
        clanId = packet.getInt()
        mapName = packet.getTerminatedString()
        // C++: Act() is called here to process the join; Kotlin server handles this separately.
    }
}
