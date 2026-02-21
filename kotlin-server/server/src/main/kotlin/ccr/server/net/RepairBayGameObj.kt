package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: RepairBayGameObj (repairbay.cpp) — extends BuildingGameObj.
// Export_Creation: calls super.exportCreation(packet), then writes repair zone center, extent, and facing.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj → RepairBayGameObj
class RepairBayGameObj(
    definitionId: Int,
    position: Vector3,
    sphereCenter: Vector3,
    sphereRadius: Float,
    health: Float = 5000f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    isDestroyed: Boolean = false,
    isPowerOn: Boolean = true,
    currentState: Int = 0,
    val repairZoneCenter: Vector3 = Vector3(0f, 0f, 0f),
    val repairZoneExtent: Vector3 = Vector3(0f, 0f, 0f),
    val repairZoneFacing: Float = 0f,
) : BuildingGameObj(
    definitionId   = definitionId,
    position       = position,
    sphereCenter   = sphereCenter,
    sphereRadius   = sphereRadius,
    health         = health,
    shieldStrength = shieldStrength,
    shieldType     = shieldType,
    isDestroyed    = isDestroyed,
    isPowerOn      = isPowerOn,
    currentState   = currentState,
) {
    // C++: RepairBayGameObj::Export_Creation — calls super then appends repair zone fields.
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)   // BuildingGameObj: definitionId + position + sphereCenter + radius
        packet.addFloat(repairZoneCenter.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(repairZoneCenter.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(repairZoneCenter.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(repairZoneExtent.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(repairZoneExtent.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(repairZoneExtent.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(repairZoneFacing)   // raw float, Z-axis rotation
    }
}
