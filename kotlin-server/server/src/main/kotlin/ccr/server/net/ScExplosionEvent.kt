package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.bitstream.*

// C++: cScExplosionEvent (scexplosionevent.h/.cpp) — networkClassId = NETCLASSID_SCEXPLOSIONEVENT = 1014
// Server-to-client one-shot event that triggers an explosion effect on the client.
// Export_Creation: DefID(int) + Position.X(BITPACK_WORLD_POSITION_X) + Position.Y(BITPACK_WORLD_POSITION_Y)
//                 + Position.Z(BITPACK_WORLD_POSITION_Z) + OwnerID(int)
class ScExplosionEvent(
    val defId: Int,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val ownerId: Int,
) : NetEvent() {
    override val networkClassId: Int = 1014

    override fun exportCreation(packet: BitStream) {
        packet.addInt(defId)
        packet.addFloat(posX, BITPACK_WORLD_POSITION_X)
        packet.addFloat(posY, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(posZ, BITPACK_WORLD_POSITION_Z)
        packet.addInt(ownerId)
        setDeletePending()  // C++: Export_Creation calls Set_Delete_Pending — one-shot event
    }
}
