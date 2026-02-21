package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: PhysicalGameObj (physicalgameobj.cpp)
// Adds position, facing, and model/animation render state.
// C++ hierarchy: DamageableGameObj → PhysicalGameObj (PhysicalGameObj is BELOW DamageableGameObj)
// playerType is a virtual property — SmartGameObj overrides it; PhysicalGameObj::Export_Rare
// writes it via Get_Player_Type() virtual dispatch (same pattern).
abstract class PhysicalGameObj(
    definitionId: Int,
    var position: Vector3,
    var facing: Float = 0f,
    var modelName: String = "",
    var animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
) : DamageableGameObj(definitionId, health, shieldStrength, shieldType) {

    open val playerType: Int get() = 0

    // C++: PhysicalGameObj::Export_Creation — position + facing.
    // definitionId is written here (C++: factory writes it via Prep_Packet before Export_Creation;
    // we include it here since our NetworkObjectPacketWriter has no factory layer).
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)  // DamageableGameObj::Export_Creation → empty chain
        packet.addInt(definitionId)
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(facing)  // raw IEEE 754, unquantized
    }

    // C++: PhysicalGameObj::Export_Rare — model, anim state, host attachment, player_type, hud_pokable.
    // player_type is written here via Get_Player_Type() virtual in C++; we use the playerType property.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)  // DamageableGameObj::Export_Rare → empty chain
        packet.addTerminatedString(modelName, permitEmpty = true)
        packet.addTerminatedString(animName, permitEmpty = true)  // anim_name
        packet.addInt(0)        // curr_frame
        packet.addInt(0)        // target_frame
        packet.addInt(0)        // anim_mode (0 = ANIM_MODE_TARGET)
        packet.addInt(0)        // host_model_id (not attached)
        packet.addInt(0)        // host_bone
        packet.addInt(playerType)   // Get_Player_Type() — overridden by SmartGameObj
        packet.addBool(false)       // hud_pokable (HUDPokableIndicatorEnabled)
        // Note: hidden bool only written for VehicleGameObj — not applicable here
    }

    // C++: PhysicalGameObj::Export_Frequent — on_host_bone flag only.
    override fun exportFrequent(packet: BitStream) {
        packet.addBool(false)   // on_host_bone
    }
}
