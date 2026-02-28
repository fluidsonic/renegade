package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.server.defs.BeaconGameObjDef
import ccr.server.defs.CNCModeSettingsDef
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseControllerClassTest {

    /** Captures playAnnouncement calls without needing a real Network. */
    private class CapturingBaseController(playerType: Int = 0) : BaseControllerClass(playerType) {
        val broadcastedIds = mutableListOf<Int>()
        override fun playAnnouncement(textId: Int) {
            broadcastedIds.add(textId)
        }
    }

    /** Creates a BeaconGameObj with a real BeaconGameObjDef so getDefinition() works. */
    private fun makeBeacon(isNuke: Boolean, playerType: Int): BeaconGameObj {
        val def = BeaconGameObjDef(
            name = "test_beacon", id = 1u, chunkId = BeaconGameObjDef.CHUNK_ID,
            isNuke = if (isNuke) 1 else 0,
        )
        val beacon = BeaconGameObj()
        beacon.definition = def
        beacon.playerType = playerType
        return beacon
    }

    /** A CNCModeSettingsDef with distinct non-zero IDs for every beacon field. */
    private val testCncDef = CNCModeSettingsDef(
        name = "test", id = 1u, chunkId = CNCModeSettingsDef.CHUNK_ID,
        nodNukeBeaconDeployedId  = 101,
        gdiNukeBeaconDeployedId  = 102,
        nodIonBeaconDeployedId   = 103,
        gdiIonBeaconDeployedId   = 104,
        nodNukeBeaconDisarmedId  = 201,
        gdiNukeBeaconDisarmedId  = 202,
        nodIonBeaconDisarmedId   = 203,
        gdiIonBeaconDisarmedId   = 204,
        nodNukeBeaconWarningId   = 301,
        gdiNukeBeaconWarningId   = 302,
        nodIonBeaconWarningId    = 303,
        gdiIonBeaconWarningId    = 304,
    )

    @Test fun `onBeaconArmed - nuke beacon NOD team broadcasts nodNukeBeaconDeployedId`() {
        val ctrl = CapturingBaseController()
        val beacon = makeBeacon(isNuke = true, playerType = 0 /* NOD */)
        ctrl.onBeaconArmed(beacon, testCncDef)
        assertEquals(listOf(101), ctrl.broadcastedIds)
    }

    @Test fun `onBeaconArmed - ion beacon GDI team broadcasts gdiIonBeaconDeployedId`() {
        val ctrl = CapturingBaseController()
        val beacon = makeBeacon(isNuke = false, playerType = 1 /* GDI */)
        ctrl.onBeaconArmed(beacon, testCncDef)
        assertEquals(listOf(104), ctrl.broadcastedIds)
    }

    @Test fun `onBeaconDisarmed - nuke beacon GDI team broadcasts gdiNukeBeaconDisarmedId`() {
        val ctrl = CapturingBaseController()
        val beacon = makeBeacon(isNuke = true, playerType = 1 /* GDI */)
        ctrl.onBeaconDisarmed(beacon, testCncDef)
        assertEquals(listOf(202), ctrl.broadcastedIds)
    }

    @Test fun `onBeaconWarning - ion beacon NOD team broadcasts nodIonBeaconWarningId`() {
        val ctrl = CapturingBaseController()
        val beacon = makeBeacon(isNuke = false, playerType = 0 /* NOD */)
        ctrl.onBeaconWarning(beacon, testCncDef)
        assertEquals(listOf(303), ctrl.broadcastedIds)
    }

    @Test fun `onBeaconArmed - zero ID is not broadcast`() {
        val cncDef = CNCModeSettingsDef(
            name = "empty", id = 2u, chunkId = CNCModeSettingsDef.CHUNK_ID,
            nodNukeBeaconDeployedId = 0,
        )
        val ctrl = CapturingBaseController()
        val beacon = makeBeacon(isNuke = true, playerType = 0)
        ctrl.onBeaconArmed(beacon, cncDef)
        assertTrue(ctrl.broadcastedIds.isEmpty())
    }
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
