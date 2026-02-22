package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cPlayerKill — networkClassId = NETCLASSID_PLAYERKILL = 1002
// BIT_CREATION: [KillerId: int] [VictimId: int]
class PlayerKill(
    val killerId: Int,
    val victimId: Int,
) : NetEvent() {
    override val networkClassId: Int = 1002

    override fun exportCreation(packet: BitStream) {
        packet.addInt(killerId)
        packet.addInt(victimId)
    }
}
