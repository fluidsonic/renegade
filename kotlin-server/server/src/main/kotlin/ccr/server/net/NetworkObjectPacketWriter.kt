package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.net.replication.NetworkObjectFactoryManager

// C++: messages.cpp Send_Object_Update — writes the wire envelope for a network object packet.
// Wire layout:
//   [networkId:32][dirtyBits:8][isDeletePending:1]
//   + [networkClassId:32]  only when BIT_CREATION (0x08) is set
//   + export tiers determined by dirty bits
//
// isDeletePending may be combined with any dirty-bit level in a single packet,
// matching C++ Send_Object_Update which writes both fields from the same object in one call.
// Keeping envelope logic here mirrors the C++ architecture where Send_Object_Update in
// messages.cpp handles framing, not the objects themselves.
object NetworkObjectPacketWriter {

    // Writes a full creation packet for a NetworkObject (dirtyBits = BIT_CREATION = 0x0F).
    // C++: messages.cpp Send_Object_Update with all 4 tiers dirty.
    // networkId is passed explicitly because NetworkObject.networkId is internal to the net module.
    fun writeCreation(bs: BitStream, obj: NetworkObject, networkId: Int, isDeletePending: Boolean = false) {
        bs.addInt(networkId)
        bs.addByte(NetworkObject.BIT_CREATION.toByte())  // dirtyBits = 0x0F (BYTE, 8 bits)
        bs.addBool(isDeletePending)
        bs.addInt(obj.networkClassId)                     // networkClassId (BIT_CREATION set)
        // C++: factory->Prep_Packet(object, packet) — writes definitionId for game objects (classId=1000)
        val factory = NetworkObjectFactoryManager.getFactory(obj.networkClassId)
        requireNotNull(factory) { "No factory registered for classId=${obj.networkClassId}" }
        factory.prepPacket(obj, bs)
        obj.exportCreation(bs)
        obj.exportRare(bs)
        obj.exportOccasional(bs)
        obj.exportFrequent(bs)
    }

    // Writes a rare-update packet for an existing NetworkObject (dirtyBits = BIT_RARE = 0x07).
    // C++: messages.cpp Send_Object_Update with BIT_RARE set (cascades: RARE|OCCASIONAL|FREQUENT).
    // No networkClassId is written — the object was already created by an earlier creation packet.
    fun writeRareUpdate(bs: BitStream, obj: NetworkObject, networkId: Int, isDeletePending: Boolean = false) {
        bs.addInt(networkId)
        bs.addByte(NetworkObject.BIT_RARE.toByte())  // dirtyBits = 0x07 (BYTE, 8 bits)
        bs.addBool(isDeletePending)
        // No networkClassId — BIT_CREATION (0x08) is not set
        obj.exportRare(bs)
        obj.exportOccasional(bs)
        obj.exportFrequent(bs)
    }

    // Writes an occasional-update packet (dirtyBits = BIT_OCCASIONAL = 0x03).
    // C++: Send_Object_Update with BIT_OCCASIONAL set (cascades: OCCASIONAL|FREQUENT).
    fun writeOccasionalUpdate(bs: BitStream, obj: NetworkObject, networkId: Int, isDeletePending: Boolean = false) {
        bs.addInt(networkId)
        bs.addByte(NetworkObject.BIT_OCCASIONAL.toByte())  // dirtyBits = 0x03
        bs.addBool(isDeletePending)
        // No networkClassId — BIT_CREATION (0x08) is not set
        obj.exportOccasional(bs)
        obj.exportFrequent(bs)
    }

    // Writes a frequent-update packet (dirtyBits = BIT_FREQUENT = 0x01).
    // C++: Send_Object_Update with only BIT_FREQUENT set.
    // Normally sent unreliably; pass isDeletePending=true to combine with deletion (forces reliable).
    fun writeFrequentUpdate(bs: BitStream, obj: NetworkObject, networkId: Int, isDeletePending: Boolean = false) {
        bs.addInt(networkId)
        bs.addByte(NetworkObject.BIT_FREQUENT.toByte())  // dirtyBits = 0x01
        bs.addBool(isDeletePending)
        // No networkClassId — BIT_CREATION (0x08) is not set
        obj.exportFrequent(bs)
    }
}
