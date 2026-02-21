package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: ArmedGameObj (armedgameobj.cpp)
// Adds targeting position. Weapon list is written by SoldierGameObj (not ArmedGameObj in C++).
// C++ hierarchy: PhysicalGameObj → ArmedGameObj (ArmedGameObj directly extends PhysicalGameObj)
abstract class ArmedGameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    var targeting: Vector3 = Vector3(position.x + 1f, position.y + 1f, position.z + 1f),
) : PhysicalGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    // C++: ArmedGameObj::Export_Frequent — calls super (PhysicalGameObj: on_host_bone) then targeting.
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)  // PhysicalGameObj: on_host_bone
        packet.addFloat(targeting.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(targeting.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(targeting.z, BITPACK_WORLD_POSITION_Z)
    }
}
