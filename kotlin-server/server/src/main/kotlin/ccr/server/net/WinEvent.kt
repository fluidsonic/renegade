package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cWinEvent — classId = NETCLASSID_WIN = 1003
// BIT_CREATION:
//   Winner (int)
//   Loser (int)
//   HostedGameNumber (int)
//   IsMapCycleOver (bool)
//   WinType (int)
//   GameDuration (int)    — DWORD, stored as int
//   MvpName (wideString)  — permitEmpty
//   MvpCount (int)
//   ModNameCrc (int)      — stored as int (uint32 same bits)
//   MapNameCrc (int)
class WinEvent(
    val winner: Int,
    val loser: Int,
    val hostedGameNumber: Int,
    val isMapCycleOver: Boolean,
    val winType: Int,
    val gameDuration: Int,
    val mvpName: String,
    val mvpCount: Int,
    val modNameCrc: Int,
    val mapNameCrc: Int,
) : NetEvent() {
    override val networkClassId: Int = 1003

    override fun exportCreation(packet: BitStream) {
        packet.addInt(winner)
        packet.addInt(loser)
        packet.addInt(hostedGameNumber)
        packet.addBool(isMapCycleOver)
        packet.addInt(winType)
        packet.addInt(gameDuration)
        packet.addWideString(mvpName, permitEmpty = true)
        packet.addInt(mvpCount)
        packet.addInt(modNameCrc)
        packet.addInt(mapNameCrc)
    }
}
