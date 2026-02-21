package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: BuildingGameObj (building.cpp)
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → BuildingGameObj
// NOTE: BuildingGameObj does NOT extend PhysicalGameObj — it has its own position/sphere.
//
// definitionId is normally written by NetworkGameObjectFactoryClass::Prep_Packet before
// Export_Creation is called in C++. In our Kotlin approach (no factory layer), we write it
// explicitly at the start of exportCreation, matching PhysicalGameObj's convention.
open class BuildingGameObj(
    definitionId: Int,
    val position: Vector3,
    val sphereCenter: Vector3,
    val sphereRadius: Float,
    health: Float = 5000f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    val isDestroyed: Boolean = false,
    val isPowerOn: Boolean = true,
    val currentState: Int = 0,   // BuildingStateClass::HEALTH100_POWERON
) : DamageableGameObj(definitionId, health, shieldStrength, shieldType) {

    // C++: BuildingGameObj::Export_Creation — calls DamageableGameObj::Export_Creation (empty chain),
    // then writes position and CollectionSphere (center + radius).
    // definitionId is written here because our Kotlin design has no factory Prep_Packet layer.
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)  // DamageableGameObj → BaseGameObj → NetworkObject — all empty
        packet.addInt(definitionId)                               // Written by factory Prep_Packet in C++
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)    // Position.X
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)    // Position.Y
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)    // Position.Z
        packet.addFloat(sphereCenter.x, BITPACK_WORLD_POSITION_X)  // CollectionSphere.Center.X
        packet.addFloat(sphereCenter.y, BITPACK_WORLD_POSITION_Y)  // CollectionSphere.Center.Y
        packet.addFloat(sphereCenter.z, BITPACK_WORLD_POSITION_Z)  // CollectionSphere.Center.Z
        packet.addFloat(sphereRadius, BITPACK_BUILDING_RADIUS)      // CollectionSphere.Radius
    }

    // C++: BuildingGameObj::Export_Rare — calls DamageableGameObj::Export_Rare (empty chain),
    // then writes IsDestroyed, IsPowerOn, CurrentState.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)  // DamageableGameObj → BaseGameObj → NetworkObject — all empty
        packet.addBool(isDestroyed)                               // IsDestroyed
        packet.addBool(isPowerOn)                                 // IsPowerOn
        packet.addInt(currentState, BITPACK_BUILDING_STATE)       // CurrentState
    }
}
