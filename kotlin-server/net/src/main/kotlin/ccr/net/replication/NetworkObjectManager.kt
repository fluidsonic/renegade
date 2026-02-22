package ccr.net.replication

// C++: NetworkObjectMgrClass in wwnet/networkobjectmgr.h
// Registry for all network objects. Handles ID allocation, think loop, and deletion.

// C++: ID ranges from networkobjectmgr.h
private const val NETID_DYNAMIC_OBJECT_MIN = 1_500_000_000  // 600M slots for server-created
private const val NETID_DYNAMIC_OBJECT_MAX = 2_100_000_000
private const val NETID_STATIC_OBJECT_MIN  = 2_100_000_001  // 10M slots for static level objects
private const val NETID_STATIC_OBJECT_MAX  = 2_110_000_000
private const val NETID_CLIENT_OBJECT_MIN  = 2_110_000_001  // 100K slots per client, 128 clients
private const val NETID_CLIENT_OBJECT_MAX  = 2_122_800_001
private const val CLIENT_ID_RANGE          = 100_000

object NetworkObjectManager {
    // C++: _ObjectList — sorted by NetworkID for binary search
    private val objectList = mutableListOf<NetworkObject>()

    // C++: _DeletePendingList
    private val deletePendingList = mutableListOf<NetworkObject>()

    // C++: _NewDynamicID
    private var newDynamicId: Int = NETID_DYNAMIC_OBJECT_MIN

    // C++: _NewClientID — set per-client by Init_New_Client_ID
    private var newClientId: Int = NETID_CLIENT_OBJECT_MIN

    // C++: _IsLevelLoading
    var isLevelLoading: Boolean = false

    // C++: Register_Object — adds object to the list, assigns or confirms its ID
    fun registerObject(obj: NetworkObject) {
        objectList.add(obj)
        objectList.sortBy { it.networkId }
    }

    // Register with explicit ID — sets networkId (internal set within net module) then registers.
    // Used by server-side code which cannot set networkId directly from outside this module.
    fun registerObject(obj: NetworkObject, networkId: Int) {
        obj.networkId = networkId
        objectList.add(obj)
        objectList.sortBy { it.networkId }
    }

    // Returns a snapshot of all registered objects for iteration.
    fun getAllObjects(): List<NetworkObject> = objectList.toList()

    // C++: Unregister_Object — removes object from the list
    fun unregisterObject(obj: NetworkObject) {
        objectList.remove(obj)
    }

    // C++: Register_Object_For_Deletion
    fun registerObjectForDeletion(obj: NetworkObject) {
        deletePendingList.add(obj)
    }

    // C++: Think — called each network tick
    fun think() {
        for (obj in objectList) {
            obj.networkThink()
        }
    }

    // C++: Delete_Pending — deletes all objects registered for deletion
    fun deletePending() {
        for (obj in deletePendingList) {
            unregisterObject(obj)
            obj.delete()
        }
        deletePendingList.clear()
    }

    // C++: Delete_Client_Objects — removes all objects owned by the given client
    fun deleteClientObjects(clientId: Int) {
        val rangeMin = NETID_CLIENT_OBJECT_MIN + (clientId - 1) * CLIENT_ID_RANGE
        val rangeMax = rangeMin + CLIENT_ID_RANGE
        val toDelete = objectList.filter { it.networkId in rangeMin until rangeMax }
        for (obj in toDelete) {
            objectList.remove(obj)
            obj.delete()
        }
    }

    // C++: Set_All_Delete_Pending — marks every object for deletion
    fun setAllDeletePending() {
        for (obj in objectList) {
            obj.setDeletePending()
        }
    }

    // C++: Restore_Dirty_Bits — marks all objects as fully dirty for a reconnecting client
    fun restoreDirtyBits(clientId: Int) {
        for (obj in objectList) {
            obj.setObjectDirtyBit(clientId, obj.creationDirtyBit, true)
        }
    }

    // C++: Find_Object — linear search (C++ uses binary search on sorted list)
    fun findObject(objectId: Int): NetworkObject? {
        return objectList.firstOrNull { it.networkId == objectId }
    }

    // C++: Get_Object_Count
    fun getObjectCount(): Int = objectList.size

    // C++: Get_Object
    fun getObject(index: Int): NetworkObject = objectList[index]

    // C++: Get_New_Dynamic_ID — allocates the next server-side dynamic ID
    fun getNewDynamicId(): Int {
        val id = newDynamicId
        newDynamicId = if (newDynamicId < NETID_DYNAMIC_OBJECT_MAX) newDynamicId + 1
                       else NETID_DYNAMIC_OBJECT_MIN
        return id
    }

    // C++: Get_Current_Dynamic_ID
    fun getCurrentDynamicId(): Int = newDynamicId

    // C++: Set_New_Dynamic_ID
    fun setNewDynamicId(id: Int) {
        newDynamicId = id
    }

    // C++: Init_New_Client_ID — sets the ID counter to the start of a client's range
    // Client IDs start at NETID_CLIENT_OBJECT_MIN + (clientId - 1) * CLIENT_ID_RANGE
    fun initNewClientId(clientId: Int) {
        newClientId = NETID_CLIENT_OBJECT_MIN + (clientId - 1) * CLIENT_ID_RANGE
    }

    // C++: Get_New_Client_ID — allocates the next ID in the current client's range
    fun getNewClientId(): Int = newClientId++

    // C++: Reset_Import_State_Counts
    fun resetImportStateCounts() {
        // Stub: ImportStateCount is per-object and not yet tracked here
    }

    // For testing — clears all state
    internal fun reset() {
        objectList.clear()
        deletePendingList.clear()
        newDynamicId = NETID_DYNAMIC_OBJECT_MIN
        newClientId = NETID_CLIENT_OBJECT_MIN
        isLevelLoading = false
    }
}
