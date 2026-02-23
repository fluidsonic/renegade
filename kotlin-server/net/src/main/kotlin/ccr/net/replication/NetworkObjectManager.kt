package ccr.net.replication

// C++: NetworkObjectMgrClass in wwnet/networkobjectmgr.h
// Registry for all network objects. Handles ID allocation, think loop, and deletion.

// C++: ID ranges from networkobjectmgr.h
private const val NETID_DYNAMIC_OBJECT_MIN = 1_500_000_000  // 600M slots for server-created
private const val NETID_DYNAMIC_OBJECT_MAX = 2_100_000_000
private const val NETID_STATIC_OBJECT_MIN  = 2_100_000_001  // 10M slots for static level objects
private const val NETID_STATIC_OBJECT_MAX  = 2_110_000_000

object NetworkObjectManager {
    const val NETID_CLIENT_OBJECT_MIN = 2_110_000_001   // C++: NETID_CLIENT_OBJECT_MIN
    const val NETID_CLIENT_OBJECT_MAX = 2_122_800_001   // C++: NETID_CLIENT_OBJECT_MAX

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

    // C++: Register_Object — inserts into sorted list only if ID != 0 and not already present
    fun registerObject(obj: NetworkObject) {
        val objectId = obj.networkId
        if (objectId != 0) {
            if (objectList.none { it.networkId == objectId }) {
                objectList.add(obj)
                objectList.sortBy { it.networkId }
            }
        }
    }

    // Register with explicit ID — delegates to setNetworkId (unregister + reassign + register).
    fun registerObject(obj: NetworkObject, networkId: Int) {
        obj.setNetworkId(networkId)
    }

    // Returns a snapshot of all registered objects for iteration.
    fun getAllObjects(): List<NetworkObject> = objectList.toList()

    // C++: Unregister_Object — removes object only if ID != 0
    fun unregisterObject(obj: NetworkObject) {
        if (obj.networkId != 0) {
            objectList.remove(obj)
        }
    }

    // C++: Register_Object_For_Deletion — adds only if not already present
    fun registerObjectForDeletion(obj: NetworkObject) {
        if (!deletePendingList.contains(obj)) {
            deletePendingList.add(obj)
        }
    }

    // C++: Think — called each network tick
    fun think() {
        for (obj in objectList) {
            obj.networkThink()
        }
    }

    // C++: Delete_Pending — skips if level loading; only deletes objects that are still pending
    fun deletePending() {
        if (isLevelLoading) return
        for (obj in deletePendingList) {
            if (obj.isDeletePending) {
                obj.delete()
            }
        }
        deletePendingList.clear()
    }

    // C++: Delete_Client_Objects — marks each client-owned object as delete pending
    fun deleteClientObjects(clientId: Int) {
        val rangeMin = NETID_CLIENT_OBJECT_MIN + (clientId - 1) * 100000
        val rangeMax = rangeMin + 100000
        for (obj in objectList) {
            if (obj.networkId in rangeMin until rangeMax) {
                obj.setDeletePending()
            }
        }
    }

    // C++: Set_All_Delete_Pending — marks every object for deletion
    fun setAllDeletePending() {
        for (obj in objectList) {
            obj.setDeletePending()
        }
    }

    // C++: Restore_Dirty_Bits — copies slot MAX_CLIENT_COUNT-1 (template slot) to the given client
    fun restoreDirtyBits(clientId: Int) {
        for (obj in objectList) {
            val genericBits = obj.getObjectDirtyBits(NetworkObject.MAX_CLIENT_COUNT - 1)
            obj.setObjectDirtyBits(clientId, genericBits)
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

    // C++: Get_New_Dynamic_ID — skips IDs already in use, then returns next
    fun getNewDynamicId(): Int {
        while (findObject(newDynamicId) != null) {
            newDynamicId++
        }
        val id = newDynamicId
        newDynamicId++
        return id
    }

    // C++: Get_Current_Dynamic_ID
    fun getCurrentDynamicId(): Int = newDynamicId

    // C++: Set_New_Dynamic_ID
    fun setNewDynamicId(id: Int) {
        newDynamicId = id
    }

    // C++: Init_New_Client_ID — sets the ID counter to the start of a client's range
    fun initNewClientId(clientId: Int) {
        newClientId = NETID_CLIENT_OBJECT_MIN + (clientId - 1) * 100000
    }

    // C++: Get_New_Client_ID — allocates the next ID in the current client's range
    fun getNewClientId(): Int = newClientId++

    // C++: Reset_Import_State_Counts
    fun resetImportStateCounts() {
        for (obj in objectList) {
            obj.resetImportStateCount()
        }
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
