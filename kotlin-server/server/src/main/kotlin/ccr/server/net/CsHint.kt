package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: cCsHint — classId = NETCLASSID_CSHINT = 1036
// Client→Server event carrying a hint/objective notification from the client.
// Wire format (BIT_CREATION):
//   SenderId (int)
//   SubjectId (int)
class CsHint(
    var senderId: Int = 0,
    var subjectId: Int = 0,
) : NetEvent() {
    override val networkClassId: Int = 1036

    override fun exportCreation(packet: BitStream) {
        packet.addInt(senderId)
        packet.addInt(subjectId)
    }

    override fun importCreation(packet: BitStream) {
        senderId = packet.getInt()
        subjectId = packet.getInt()
    }
}
