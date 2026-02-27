package ccr.net.replication

import ccr.math.Vector3
import ccr.net.timeGetTime
import ccr.net.bitstream.BitStream

// C++: NetworkObjectClass in wwnet/networkobject.h
// Base class for all objects that transmit state updates across the network.
// Uses a 4-tier dirty bit system for per-client update tracking.

private const val CLIENT_SIDE_UPDATE_FREQUENCY_SAMPLE_PERIOD = 10_000  // C++: (1000 * 10) ms

// C++: PACKET_TIER_ENUM
const val PACKET_TIER_COUNT = 4
enum class PacketTier(val value: Int) {
    CREATION(0),    // Full creation data — cascades: sets RARE, OCCASIONAL, FREQUENT
    RARE(1),        // Rarely changing data — cascades: sets OCCASIONAL, FREQUENT
    OCCASIONAL(2),  // Occasionally changing data — cascades: sets FREQUENT
    FREQUENT(3),    // Frequently changing data
}

// C++: PerClientUpdateInfoStruct
class PerClientUpdateInfo {
    var lastUpdateTime: UInt = 0u       // C++: unsigned long LastUpdateTime
    var updateRate: UShort = 0u         // C++: unsigned short UpdateRate
    var clientHintCount: Byte = 0       // C++: BYTE ClientHintCount
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

        // C++: static bool IsServer
        var isServer: Boolean = false
    }

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
    var frequentExportPacketSize: UByte = 0u

    // C++: UnreliableOverride — send frequent updates unreliably if true
    var unreliableOverride: Boolean = false

    // C++: PerClientUpdateInfoStruct UpdateInfo[MAX_CLIENT_COUNT]
    private val updateInfo = Array(MAX_CLIENT_COUNT) { PerClientUpdateInfo() }

    // client-only: C++: ImportStateCount
    private var importStateCount: Int = 0

    // client-only: C++: LastClientsideUpdateTime
    private var lastClientsideUpdateTime: UInt = 0u

    // client-only: C++: ClientsideUpdateFrequencySampleStartTime
    private var clientsideUpdateFrequencySampleStartTime: UInt = timeGetTime()

    // client-only: C++: ClientsideUpdateFrequencySampleCount
    private var clientsideUpdateFrequencySampleCount: Int = 0

    // client-only: C++: ClientsideUpdateRate
    private var clientsideUpdateRate: Int = 0

    // C++: LastObjectIdIDamaged
    var lastObjectIdIDamaged: Int = -1

    // C++: LastObjectIdIGotDamagedBy
    var lastObjectIdIGotDamagedBy: Int = -1

    // C++: CachedPriority (private in C++)
    private var cachedPriority: Float = 0f

    // C++: CachedPriority_2[MAX_CLIENT_COUNT]
    private val cachedPriority2 = FloatArray(MAX_CLIENT_COUNT)

    init {
        if (isServer) {
            // C++: constructor body — matches Set_Network_ID(Get_New_Dynamic_ID())
            val newId = NetworkObjectManager.getNewDynamicId()
            setNetworkId(newId)
        }
        clearObjectDirtyBits()
    }

    // C++: Get_Network_Class_ID — identifies the object type for the factory
    open val networkClassId: Int get() = 0

    // C++: Set_Network_ID — unregisters, changes ID, re-registers
    fun setNetworkId(id: Int) {
        require(id > 0)
        NetworkObjectManager.unregisterObject(this)
        networkId = id
        NetworkObjectManager.registerObject(this)
    }

    // C++: ~NetworkObjectClass destructor — unregisters from manager
    // FIXME: not called yet — wire into deletion pipeline to match C++ destructor auto-unregister
    open fun destruct() {
        NetworkObjectManager.unregisterObject(this)
    }

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

    // C++: Set_Delete_Pending — marks for deletion and registers with manager
    open fun setDeletePending() {
        isDeletePending = true
        NetworkObjectManager.registerObjectForDeletion(this)
    }

    // C++: Delete — pure virtual; override to clean up the object
    // FIXME: should be called via destruct() pipeline, not directly
    abstract fun delete()

    // C++: Get_Vis_ID
    open fun getVisId(): Int = -1

    // C++: Get_World_Position(Vector3&) — Kotlin-idiomatic nullable return
    open fun getWorldPosition(): Vector3? = null

    // C++: Get_Filter_Distance
    open fun getFilterDistance(): Float = 10000.0f

    // C++: Is_Tagged
    open fun isTagged(): Boolean = false

    // C++: Get_Description(StringClass&) — Kotlin-idiomatic String return
    open fun getDescription(): String = ""

    // C++: Set_Object_Dirty_Bit(dirty_bit, onoff) — marks all clients dirty (0..MAX-1)
    // C++ has no isServer guard and includes client 0 (the server slot is index 0 internally).
    open fun setObjectDirtyBit(dirtyBit: Int, on: Boolean) {
        for (i in 0 ..< MAX_CLIENT_COUNT) {
            if (on) clientStatus[i] = (clientStatus[i].toInt() or dirtyBit).toByte()
            else    clientStatus[i] = (clientStatus[i].toInt() and dirtyBit.inv()).toByte()
        }
    }

    // C++: Set_Object_Dirty_Bit(client_id, dirty_bit, onoff) — per-client
    open fun setObjectDirtyBit(clientId: Int, dirtyBit: Int, on: Boolean) {
        val cur = clientStatus[clientId].toInt() and 0xFF
        clientStatus[clientId] = if (on) (cur or dirtyBit).toByte()
                                 else (cur and dirtyBit.inv()).toByte()
    }

    // C++: Clear_Object_Dirty_Bits — also resets per-client update info
    open fun clearObjectDirtyBits() {
        clientStatus.fill(0)
        for (info in updateInfo) {
            info.lastUpdateTime = 0u
            info.updateRate = 50u
            info.clientHintCount = 0
        }
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

    // --- Client hint count ---

    // C++: Reset_Client_Hint_Count
    fun resetClientHintCount(clientId: Int) {
        require(clientId >= 0 && clientId < MAX_CLIENT_COUNT)
        updateInfo[clientId].clientHintCount = 0
    }

    // C++: Increment_Client_Hint_Count
    fun incrementClientHintCount(clientId: Int) {
        require(clientId >= 0 && clientId < MAX_CLIENT_COUNT)
        if (updateInfo[clientId].clientHintCount < 255)
            updateInfo[clientId].clientHintCount++
    }

    // C++: Hint_To_All_Clients
    fun hintToAllClients() {
        for (i in 0 until MAX_CLIENT_COUNT) incrementClientHintCount(i)
    }

    // C++: Get_Client_Hint_Count
    fun getClientHintCount(clientId: Int): Byte {
        require(clientId >= 0 && clientId < MAX_CLIENT_COUNT)
        return updateInfo[clientId].clientHintCount
    }

    // --- Import state count (client-only) ---

    // client-only: C++: Reset_Import_State_Count
    fun resetImportStateCount() { importStateCount = 0 }

    // client-only: C++: Increment_Import_State_Count
    fun incrementImportStateCount() { importStateCount++ }

    // client-only: C++: Get_Import_State_Count
    fun getImportStateCount(): Int = importStateCount

    // --- Clientside update time/frequency (client-only) ---

    // client-only: C++: Reset_Last_Clientside_Update_Time
    fun resetLastClientsideUpdateTime() {
        lastClientsideUpdateTime = 0u
        clientsideUpdateFrequencySampleStartTime = timeGetTime()
        clientsideUpdateFrequencySampleCount = 0
    }

    // client-only: C++: Set_Last_Clientside_Update_Time
    fun setLastClientsideUpdateTime(time: UInt) {
        lastClientsideUpdateTime = time
        clientsideUpdateFrequencySampleCount++
    }

    // client-only: C++: Get_Last_Clientside_Update_Time
    fun getLastClientsideUpdateTime(): UInt = lastClientsideUpdateTime

    // client-only: C++: Get_Clientside_Update_Frequency (rolling 10s average)
    fun getClientsideUpdateFrequency(): Int {
        val time = timeGetTime()
        if (time - clientsideUpdateFrequencySampleStartTime > CLIENT_SIDE_UPDATE_FREQUENCY_SAMPLE_PERIOD.toUInt()) {
            var rate = 10000
            if (clientsideUpdateFrequencySampleCount != 0) {
                rate = ((time - clientsideUpdateFrequencySampleStartTime) / clientsideUpdateFrequencySampleCount.toUInt()).toInt()
                clientsideUpdateFrequencySampleStartTime = time
                clientsideUpdateFrequencySampleCount = 0
            }
            clientsideUpdateRate = rate
        }
        return clientsideUpdateRate
    }

    // --- Per-client update time and rate ---

    // C++: Get_Last_Update_Time
    fun getLastUpdateTime(clientId: Int): UInt {
        require(clientId > 0 && clientId <= MAX_CLIENT_COUNT)
        return updateInfo[clientId].lastUpdateTime
    }

    // C++: Set_Last_Update_Time
    fun setLastUpdateTime(clientId: Int, time: UInt) {
        require(clientId > 0 && clientId <= MAX_CLIENT_COUNT)
        updateInfo[clientId].lastUpdateTime = time
    }

    // C++: Get_Update_Rate
    fun getUpdateRate(clientId: Int): UShort {
        require(clientId > 0 && clientId <= MAX_CLIENT_COUNT)
        return updateInfo[clientId].updateRate
    }

    // C++: Set_Update_Rate
    fun setUpdateRate(clientId: Int, rate: UShort) {
        require(clientId > 0 && clientId <= MAX_CLIENT_COUNT)
        updateInfo[clientId].updateRate = rate
    }

    // --- Ownership ---

    // C++: Belongs_To_Client
    fun belongsToClient(clientId: Int): Boolean {
        require(clientId > 0)
        val idMin = NetworkObjectManager.NETID_CLIENT_OBJECT_MIN + (clientId - 1) * 100000
        val idMax = idMin + 100000 - 1
        return networkId in idMin..idMax
    }

    // --- Priority caching ---

    // C++: Set_Cached_Priority
    fun setCachedPriority(priority: Float) {
        require(priority >= 0f && priority <= 1f)
        cachedPriority = priority
    }

    // C++: Get_Cached_Priority (virtual)
    open fun getCachedPriority(): Float = cachedPriority

    // C++: Set_Cached_Priority_2
    fun setCachedPriority2(clientId: Int, priority: Float) {
        cachedPriority2[clientId] = priority
    }

    // C++: Get_Cached_Priority_2
    fun getCachedPriority2(clientId: Int): Float = cachedPriority2[clientId]
}
