package ccr.server.net

import ccr.net.bitstream.*
import ccr.net.replication.NetworkObject
import ccr.math.Vector3
import ccr.server.Network

// C++: CClientControl (Combat/clientcontrol.h) — networkClassId = NETCLASSID_CLIENTCONTROL = 1017
// C→S mirrored object to represent client control and targeting data.
// The server only imports (reads) this object; the client exports (writes) it.
//
// Wire format:
//   Export_Creation: Add(ClientId: int)
//   Import_Creation: Get(ClientId: int)
//   Export_Frequent: Add(SmartObjId: int); if SmartObjId != -1: control+state data follows
//   Import_Frequent: Get(SmartObjId: int); if SmartObjId != -1: ControlClass::Import_Cs + Import_State_Cs
class ClientControl(
    var clientId: Int = 0,
    var smartObjId: Int = -1,
) : NetworkObject() {
    override val networkClassId: Int = 1017
    override fun delete() {}

    // Server reference — set by serverPacketHandler after factory creation.
    var server: Network? = null
    // Server-trusted host ID (set by serverPacketHandler from the packet source).
    // Used for all lookups instead of clientId (which is client-supplied in importCreation).
    var rhostId: Int = 0

    override fun exportCreation(packet: BitStream) {
        packet.addInt(clientId)
    }

    override fun importCreation(packet: BitStream) {
        clientId = packet.getInt()
    }

    override fun exportFrequent(packet: BitStream) {
        packet.addInt(smartObjId)
        // When smartObjId != -1, the client would normally write Export_Control_Cs + Export_State_Cs here.
        // The server-side implementation does not export frequent data with non-null objects.
    }

    override fun importFrequent(packet: BitStream) {
        smartObjId = try { packet.getInt() } catch (e: Exception) { return }
        if (smartObjId == -1) return  // no controlled object

        val srv = server ?: run { packet.flush(); return }
        val soldier = srv.god.soldiersByHost[rhostId] ?: run { packet.flush(); return }
        if (soldier.networkId != smartObjId) {
            // SmartObjId doesn't match — client may not be controlling their soldier yet
            packet.flush()
            return
        }

        try {
            // --- ControlClass::Import_Cs ---
            val oneTimeBits = packet.getInt(BITPACK_ONE_TIME_BOOLEAN_BITS)
            val contBits = packet.getByte(BITPACK_CONTINUOUS_BOOLEAN_BITS).toInt() and 0xFF
            val fwd  = packet.getFloat(BITPACK_ANALOG_VALUES)
            val left = packet.getFloat(BITPACK_ANALOG_VALUES)
            val up   = packet.getFloat(BITPACK_ANALOG_VALUES)
            val turn = packet.getFloat(BITPACK_ANALOG_VALUES)

            // --- SoldierGameObj::Import_State_Cs ---
            val isSniping = packet.getBool()
            val checking = packet.getBool()
            if (checking) {
                packet.getInt()  // anti-cheat CRC — discard
            }

            // --- ArmedGameObj::Import_State_Cs --- (RELATIVE targeting)
            val relTx = packet.getFloat(BITPACK_WORLD_POSITION_X)
            val relTy = packet.getFloat(BITPACK_WORLD_POSITION_Y)
            val relTz = packet.getFloat(BITPACK_WORLD_POSITION_Z)
            // Reconstruct absolute: target = relative + server-known position
            val tx = relTx + soldier.position.x
            val ty = relTy + soldier.position.y
            val tz = relTz + soldier.position.z

            // Update soldier state via ControlClass API (the new hierarchy uses control object)
            soldier.targeting = Vector3(tx, ty, tz)
            soldier.control.setBoolean(ControlClass.BooleanControl.WEAPON_FIRE_PRIMARY,
                (contBits and 1) != 0)
            soldier.control.setBoolean(ControlClass.BooleanControl.WEAPON_FIRE_SECONDARY,
                (contBits and 2) != 0)
            soldier.control.setBoolean(ControlClass.BooleanControl.WALK,
                (contBits and 4) != 0)
            soldier.control.setBoolean(ControlClass.BooleanControl.CROUCH,
                (contBits and 8) != 0)
            soldier.control.setAnalog(ControlClass.AnalogControl.MOVE_FORWARD, fwd)
            soldier.control.setAnalog(ControlClass.AnalogControl.MOVE_LEFT,    left)
            soldier.control.setAnalog(ControlClass.AnalogControl.MOVE_UP,      up)
            soldier.control.setAnalog(ControlClass.AnalogControl.TURN_LEFT,    turn)

            // C4 detonation: bit 1 = BOOLEAN_WEAPON_FIRE_SECONDARY (alt-fire / remote trigger)
            val weaponFirePrimary   = (contBits and 1) != 0
            val weaponFireSecondary = (contBits and 2) != 0
            val currentWeaponDefId  = soldier.getWeapon()?.definitionId ?: 0
            soldier.detonateC4 = weaponFireSecondary && srv.isC4Weapon(currentWeaponDefId)
            if (weaponFirePrimary && srv.isC4Weapon(currentWeaponDefId)) {
                srv.god.createC4(rhostId, soldier, System.currentTimeMillis())
            }
            if (weaponFirePrimary && srv.isBeaconWeapon(currentWeaponDefId)) {
                val ammoDef = srv.getAmmoDefForWeapon(currentWeaponDefId)
                if (ammoDef != null) {
                    srv.god.createBeacon(rhostId, soldier, ammoDef, System.currentTimeMillis())
                }
            }

            // Mark BIT_FREQUENT dirty for all other in-game clients so the replication tick
            // will forward the position update unreliably
            for (otherId in srv.god.playerInGame) {
                if (otherId != rhostId) {
                    soldier.setObjectDirtyBit(otherId, NetworkObject.BIT_FREQUENT, true)
                }
            }
        } catch (e: Exception) {
            // Malformed packet — discard silently
        }
    }
}
