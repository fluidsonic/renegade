package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: SmartGameObj (smartgameobj.cpp)
// Adds control ownership and player input state.
abstract class SmartGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    targeting: Vector3 = Vector3(position.x + 1f, position.y + 1f, position.z + 1f),
    var controlOwner: Int = 0,
    var team: Int = 0,
) : ArmedGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType, targeting) {

    // C++: SmartGameObj overrides Get_Player_Type() — PhysicalGameObj::Export_Rare reads it.
    override val playerType: Int get() = team

    // C++: SmartGameObj::Export_Creation — calls super then writes control_owner.
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)  // PhysicalGameObj: definitionId + position + facing
        packet.addInt(controlOwner)   // control_owner → client sets COMBAT_STAR = this
    }

    // C++: SmartGameObj has no Export_Rare — inherits PhysicalGameObj's through the chain.

    // C++: SmartGameObj::Export_Frequent — calls super (ArmedGameObj → PhysicalGameObj) then control.
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)  // ArmedGameObj: on_host_bone + targeting
        // C++: Export_Control_Sc → ControlClass::Export_Sc (control.cpp #if 01 branch)
        // NOTE: #if 01 is truthy — sends 4 analog floats, NOT BITPACK_CONTROL_MOVES_SC.
        packet.addByte(0, BITPACK_CONTINUOUS_BOOLEAN_BITS)  // unsigned char ContinuousBooleanBits
        packet.addFloat(0f, BITPACK_ANALOG_VALUES)         // ANALOG_MOVE_FORWARD
        packet.addFloat(0f, BITPACK_ANALOG_VALUES)         // ANALOG_MOVE_LEFT
        packet.addFloat(0f, BITPACK_ANALOG_VALUES)         // ANALOG_MOVE_UP
        packet.addFloat(0f, BITPACK_ANALOG_VALUES)         // ANALOG_TURN_LEFT
    }
}
