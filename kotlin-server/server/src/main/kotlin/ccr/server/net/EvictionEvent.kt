package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cEvictionEvent — networkClassId = NETCLASSID_EVICTIONEVENT = 1009
// BIT_CREATION: [EvictionCode: int]
class EvictionEvent(
    val evictionCode: Int,
) : NetEvent() {
    override val networkClassId: Int = 1009

    override fun exportCreation(packet: BitStream) {
        packet.addInt(evictionCode)
    }
}
