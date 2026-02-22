package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.bitstream.*

// C++: cScObeliskEvent (scobeliskevent.h/.cpp) — networkClassId = NETCLASSID_SCOBELISKEVENT = 1015
// Server-to-client one-shot event that triggers an obelisk beam/explosion effect on the client.
// Same wire format as cScExplosionEvent: DefID + Position + OwnerID.
// Export_Creation: DefID(int) + Position.X(BITPACK_WORLD_POSITION_X) + Position.Y(BITPACK_WORLD_POSITION_Y)
//                 + Position.Z(BITPACK_WORLD_POSITION_Z) + OwnerID(int)
class ScObeliskEvent(
    val defId: Int,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val ownerId: Int,
) : NetEvent() {
    override val networkClassId: Int = 1015

    override fun exportCreation(packet: BitStream) {
        packet.addInt(defId)
        packet.addFloat(posX, BITPACK_WORLD_POSITION_X)
        packet.addFloat(posY, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(posZ, BITPACK_WORLD_POSITION_Z)
        packet.addInt(ownerId)
    }
}
