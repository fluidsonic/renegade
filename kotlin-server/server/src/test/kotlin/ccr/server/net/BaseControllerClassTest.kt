package ccr.server.net

import ccr.net.bitstream.BitStream
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseControllerClassTest {
    private fun defaultCtrl(
        playerType: Int = 0,
        operationTimeFactor: Float = 1.0f,
        isBasePowered: Boolean = true,
        canGenerateSoldiers: Boolean = true,
        canGenerateVehicles: Boolean = true,
        isBaseDestroyed: Boolean = false,
        didBeaconDestroyBase: Boolean = false,
        isRadarEnabled: Boolean = true,
    ): BaseControllerClass {
        val ctrl = BaseControllerClass(playerType)
        if (operationTimeFactor != 1.0f) ctrl.setOperationTimeFactor(operationTimeFactor)
        if (!isBasePowered) ctrl.setBasePowered(isBasePowered)
        if (canGenerateSoldiers) ctrl.setCanGenerateSoldiers(canGenerateSoldiers)
        if (canGenerateVehicles) ctrl.setCanGenerateVehicles(canGenerateVehicles)
        if (isBaseDestroyed) ctrl.setBaseDestroyed(isBaseDestroyed)
        if (didBeaconDestroyBase) ctrl.setBeaconDestroyedBase(didBeaconDestroyBase)
        if (!isRadarEnabled) ctrl.enableRadar(isRadarEnabled)
        return ctrl
    }

    @Test fun `occasional - field order matches C++ exactly`() {
        val bs = BitStream()
        defaultCtrl(
            operationTimeFactor = 0.75f,
            isBasePowered = false,
            canGenerateSoldiers = true,
            canGenerateVehicles = false,
            isBaseDestroyed = true,
            didBeaconDestroyBase = true,
            isRadarEnabled = false,
        ).exportOccasional(bs)

        assert(abs(bs.getFloat() - 0.75f) < 0.001f) { "operationTimeFactor expected 0.75" }
        assertFalse(bs.getBool())   // isBasePowered = false
        assertTrue(bs.getBool())    // canGenerateSoldiers = true
        assertFalse(bs.getBool())   // canGenerateVehicles = false
        assertTrue(bs.getBool())    // isBaseDestroyed = true
        assertTrue(bs.getBool())    // didBeaconDestroyBase = true
        assertFalse(bs.getBool())   // isRadarEnabled = false
    }

    @Test fun `occasional - defaults (fully operational base)`() {
        val bs = BitStream()
        defaultCtrl().exportOccasional(bs)
        assert(abs(bs.getFloat() - 1.0f) < 0.001f) { "operationTimeFactor expected 1.0" }
        assertTrue(bs.getBool())    // isBasePowered = true
        assertTrue(bs.getBool())    // canGenerateSoldiers = true
        assertTrue(bs.getBool())    // canGenerateVehicles = true
        assertFalse(bs.getBool())   // isBaseDestroyed = false
        assertFalse(bs.getBool())   // didBeaconDestroyBase = false
        assertTrue(bs.getBool())    // isRadarEnabled = true
    }

    @Test fun `setDeletePending is no-op`() {
        val ctrl = defaultCtrl()
        ctrl.setDeletePending()
        assertFalse(ctrl.isDeletePending)
    }

    @Test fun `full creation envelope`() {
        val ctrl = defaultCtrl(playerType = 1)
        val bs = BitStream()
        NetworkObjectPacketWriter.writeCreation(bs, ctrl, networkId = 400001)
        bs.getInt()   // networkId
        bs.getByte()  // dirtyBits
        bs.getBool()  // isDeletePending
        assertEquals(ctrl.networkClassId, bs.getInt())
    }
}
