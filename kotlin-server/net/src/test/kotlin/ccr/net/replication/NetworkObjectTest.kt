package ccr.net.replication

import ccr.net.bitstream.BitStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class TestNetworkObject(override val networkClassId: Int = 42) : NetworkObject() {
    var deleted = false
    override fun delete() { deleted = true }
}

class NetworkObjectTest {

    @Test
    fun `dirty bit BIT_CREATION sets all tiers`() {
        val obj = TestNetworkObject()
        obj.setObjectDirtyBit(0, NetworkObject.BIT_CREATION, true)
        assertTrue(obj.getObjectDirtyBit(0, NetworkObject.BIT_FREQUENT))
        assertTrue(obj.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
        assertTrue(obj.getObjectDirtyBit(0, NetworkObject.BIT_RARE))
        assertTrue(obj.getObjectDirtyBit(0, NetworkObject.BIT_CREATION))
    }

    @Test
    fun `dirty bit BIT_FREQUENT sets only frequent`() {
        val obj = TestNetworkObject()
        obj.setObjectDirtyBit(0, NetworkObject.BIT_FREQUENT, true)
        assertTrue(obj.getObjectDirtyBit(0, NetworkObject.BIT_FREQUENT))
        assertFalse(obj.getObjectDirtyBit(0, NetworkObject.BIT_OCCASIONAL))
        assertFalse(obj.getObjectDirtyBit(0, NetworkObject.BIT_RARE))
        assertFalse(obj.getObjectDirtyBit(0, NetworkObject.BIT_CREATION))
    }

    @Test
    fun `clearObjectDirtyBits clears all clients`() {
        val obj = TestNetworkObject()
        obj.setObjectDirtyBit(NetworkObject.BIT_CREATION, true)
        obj.clearObjectDirtyBits()
        for (i in 0 until NetworkObject.MAX_CLIENT_COUNT) {
            assertFalse(obj.isClientDirty(i))
        }
    }

    @Test
    fun `setObjectDirtyBit all clients marks all clients dirty`() {
        NetworkObject.isServer = true
        try {
            val obj = TestNetworkObject()
            obj.setObjectDirtyBit(NetworkObject.BIT_FREQUENT, true)
            // setObjectDirtyBit(dirtyBit, on) marks clients 1..MAX_CLIENT_COUNT-1;
            // client 0 is the server slot and is intentionally skipped.
            for (i in 1 until NetworkObject.MAX_CLIENT_COUNT) {
                assertTrue(obj.isClientDirty(i))
            }
        } finally {
            NetworkObject.isServer = false
        }
    }

    @Test
    fun `turning off dirty bit clears it`() {
        val obj = TestNetworkObject()
        obj.setObjectDirtyBit(0, NetworkObject.BIT_CREATION, true)
        obj.setObjectDirtyBit(0, NetworkObject.BIT_FREQUENT, false)
        // BIT_CREATION=0x0F, clearing BIT_FREQUENT=0x01 leaves 0x0E
        assertEquals(0x0E.toByte(), obj.getObjectDirtyBits(0))
    }

    @Test
    fun `setDeletePending marks object for deletion`() {
        val obj = TestNetworkObject()
        assertFalse(obj.isDeletePending)
        obj.setDeletePending()
        assertTrue(obj.isDeletePending)
    }

    @Test
    fun `manager getNewDynamicId allocates sequentially`() {
        NetworkObjectManager.reset()
        val id1 = NetworkObjectManager.getNewDynamicId()
        val id2 = NetworkObjectManager.getNewDynamicId()
        assertEquals(1_500_000_000, id1)
        assertEquals(1_500_000_001, id2)
    }

    @Test
    fun `manager registerObject and findObject`() {
        NetworkObjectManager.reset()
        val obj = TestNetworkObject()
        obj.networkId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(obj)
        val found = NetworkObjectManager.findObject(obj.networkId)
        assertNotNull(found)
        assertEquals(obj.networkId, found.networkId)
    }

    @Test
    fun `manager unregisterObject removes object`() {
        NetworkObjectManager.reset()
        val obj = TestNetworkObject()
        obj.networkId = NetworkObjectManager.getNewDynamicId()
        NetworkObjectManager.registerObject(obj)
        NetworkObjectManager.unregisterObject(obj)
        assertNull(NetworkObjectManager.findObject(obj.networkId))
    }

    @Test
    fun `manager restoreDirtyBits marks all objects as creation-dirty for client`() {
        NetworkObjectManager.reset()
        NetworkObject.isServer = true
        try {
            val obj = TestNetworkObject()
            obj.networkId = NetworkObjectManager.getNewDynamicId()
            // setObjectDirtyBit(BIT_CREATION, true) populates all client slots including the
            // template slot (MAX_CLIENT_COUNT-1) which restoreDirtyBits copies from.
            obj.setObjectDirtyBit(NetworkObject.BIT_CREATION, true)
            NetworkObjectManager.registerObject(obj)
            NetworkObjectManager.restoreDirtyBits(clientId = 3)
            assertTrue(obj.getObjectDirtyBit(3, NetworkObject.BIT_CREATION))
        } finally {
            NetworkObject.isServer = false
        }
    }

    @Test
    fun `factory creates object by classId`() {
        val factory = object : NetworkObjectFactory {
            override val classId = 42
            override fun create(packet: BitStream) = TestNetworkObject(classId)
        }
        NetworkObjectFactoryManager.register(factory)

        val obj = NetworkObjectFactoryManager.create(42, BitStream())
        assertNotNull(obj)
        assertEquals(42, obj.networkClassId)

        NetworkObjectFactoryManager.unregister(42)
    }

    @Test
    fun `factory returns null for unknown classId`() {
        assertNull(NetworkObjectFactoryManager.create(99999, BitStream()))
    }

    @Test
    fun `client ID range is 100K per client`() {
        NetworkObjectManager.reset()
        NetworkObjectManager.initNewClientId(1)
        val id1 = NetworkObjectManager.getNewClientId()
        NetworkObjectManager.initNewClientId(2)
        val id2 = NetworkObjectManager.getNewClientId()
        assertEquals(100_000, id2 - id1)
    }
}
