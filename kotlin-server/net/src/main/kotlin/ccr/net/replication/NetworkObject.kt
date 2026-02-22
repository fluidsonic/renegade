package ccr.net.replication

import ccr.net.bitstream.BitStream

// C++: NetworkObjectClass in wwnet/networkobject.h
// Base class for all objects that transmit state updates across the network.
// Uses a 4-tier dirty bit system for per-client update tracking.

// C++: PACKET_TIER_ENUM
enum class PacketTier {
    CREATION,    // Full creation data — cascades: sets RARE, OCCASIONAL, FREQUENT
    RARE,        // Rarely changing data — cascades: sets OCCASIONAL, FREQUENT
    OCCASIONAL,  // Occasionally changing data — cascades: sets FREQUENT
    FREQUENT,    // Frequently changing data
}

abstract class NetworkObject {
    companion object {
        // C++: DIRTY_BIT enum — cascading masks
        // BIT_FREQUENT   = 0x01
        // BIT_OCCASIONAL = 0x02 | BIT_FREQUENT  = 0x03
        // BIT_RARE       = 0x04 | BIT_OCCASIONAL = 0x07
        // BIT_CREATION   = 0x08 | BIT_RARE       = 0x0F
        const val BIT_FREQUENT: Int   = 0x01
        const val BIT_OCCASIONAL: Int = 0x03
        const val BIT_RARE: Int       = 0x07
        const val BIT_CREATION: Int   = 0x0F

        const val MAX_CLIENT_COUNT = 128
    }

    // The highest dirty bit level this object should be sent with on first
    // replication to a new client. Matches C++ pattern where BaseGameObj sets
    // BIT_CREATION, but NetworkObjectClass-only singletons (BaseControllerClass,
    // ServerFps) never get BIT_CREATION because they have no factory (classId=0).
    open val creationDirtyBit: Int = BIT_CREATION

    // C++: NetworkID
    var networkId: Int = 0
        internal set

    // C++: ClientStatus[MAX_CLIENT_COUNT] — per-client dirty bit mask
    private val clientStatus = ByteArray(MAX_CLIENT_COUNT)

    // C++: IsDeletePending
    var isDeletePending: Boolean = false
        protected set

    // C++: AppPacketType
    var appPacketType: Byte = 0

    // C++: FrequentExportPacketSize
    var frequentExportPacketSize: Byte = 0

    // C++: UnreliableOverride — send frequent updates unreliably if true
    var unreliableOverride: Boolean = false

    // C++: Get_Network_Class_ID — identifies the object type for the factory
    abstract val networkClassId: Int

    // C++: Import_Creation / Export_Creation — full creation state
    open fun importCreation(packet: BitStream) {}
    open fun exportCreation(packet: BitStream) {}

    // C++: Import_Rare / Export_Rare — rarely changing state
    open fun importRare(packet: BitStream) {}
    open fun exportRare(packet: BitStream) {}

    // C++: Import_Occasional / Export_Occasional — occasionally changing state
    open fun importOccasional(packet: BitStream) {}
    open fun exportOccasional(packet: BitStream) {}

    // C++: Import_Frequent / Export_Frequent — frequently changing state
    open fun importFrequent(packet: BitStream) {}
    open fun exportFrequent(packet: BitStream) {}

    // C++: Network_Think — called each network tick
    open fun networkThink() {}

    // C++: Set_Delete_Pending
    open fun setDeletePending() {
        isDeletePending = true
    }

    // C++: Delete — pure virtual; override to clean up the object
    abstract fun delete()

    // C++: Set_Object_Dirty_Bit(dirty_bit, onoff) — marks all clients dirty
    open fun setObjectDirtyBit(dirtyBit: Int, on: Boolean) {
        for (i in 0 until MAX_CLIENT_COUNT) {
            setObjectDirtyBit(i, dirtyBit, on)
        }
    }

    // C++: Set_Object_Dirty_Bit(client_id, dirty_bit, onoff) — per-client
    open fun setObjectDirtyBit(clientId: Int, dirtyBit: Int, on: Boolean) {
        val cur = clientStatus[clientId].toInt() and 0xFF
        clientStatus[clientId] = if (on) (cur or dirtyBit).toByte()
                                 else (cur and dirtyBit.inv()).toByte()
    }

    // C++: Clear_Object_Dirty_Bits
    open fun clearObjectDirtyBits() {
        clientStatus.fill(0)
    }

    // C++: Get_Object_Dirty_Bit — checks whether all bits in dirtyBit are set
    open fun getObjectDirtyBit(clientId: Int, dirtyBit: Int): Boolean {
        val bits = clientStatus[clientId].toInt() and 0xFF
        return (bits and dirtyBit) == dirtyBit
    }

    // C++: Get_Object_Dirty_Bits
    open fun getObjectDirtyBits(clientId: Int): Byte = clientStatus[clientId]

    // C++: Set_Object_Dirty_Bits
    open fun setObjectDirtyBits(clientId: Int, bits: Byte) {
        clientStatus[clientId] = bits
    }

    // C++: Is_Client_Dirty — true if any dirty bit is set for this client
    open fun isClientDirty(clientId: Int): Boolean = clientStatus[clientId] != 0.toByte()
}
