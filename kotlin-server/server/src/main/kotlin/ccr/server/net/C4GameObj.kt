package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: C4GameObj (c4gameobj.cpp) — extends SimpleGameObj.
// Export_Rare appends C4-specific fields after the SimpleGameObj/PhysicalGameObj chain.
// Hierarchy: NetworkObject → BaseGameObj → DamageableGameObj → PhysicalGameObj → SimpleGameObj → C4GameObj
class C4GameObj(
    definitionId: Int,
    position: Vector3,
    facing: Float = 0f,
    modelName: String = "",
    animName: String = "",
    health: Float = 100f,
    shieldStrength: Float = 0f,
    shieldType: Int = 0,
    // C4-specific rare fields
    val ammoDef: Int = 0,
    val ownerId: Int = 0,
    val velX: Float = 0f,
    val velY: Float = 0f,
    val velZ: Float = 0f,
    val stuck: Boolean = false,
    val stuckPosX: Float = 0f,
    val stuckPosY: Float = 0f,
    val stuckPosZ: Float = 0f,
    val stuckMct: Boolean = false,
    val stuckToObject: Boolean = false,
    val stuckObjectId: Int = 0,
    val stuckOffsetX: Float = 0f,
    val stuckOffsetY: Float = 0f,
    val stuckOffsetZ: Float = 0f,
    val stuckBone: Int = 0,
    val stuckStaticAnim: Boolean = false,
    val stuckStaticAnimObjId: Int = 0,
) : SimpleGameObj(definitionId, position, facing, modelName, animName, health, shieldStrength, shieldType) {

    // C++: C4GameObj::Export_Rare — calls super then appends C4 fields.
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)   // PhysicalGameObj: model + anim + host + player_type + hud_pokable
        packet.addInt(ammoDef)
        packet.addInt(ownerId)
        packet.addFloat(velX, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velY, BITPACK_VEHICLE_VELOCITY)
        packet.addFloat(velZ, BITPACK_VEHICLE_VELOCITY)
        packet.addBool(stuck)
        if (stuck) {
            packet.addFloat(stuckPosX, BITPACK_WORLD_POSITION_X)
            packet.addFloat(stuckPosY, BITPACK_WORLD_POSITION_Y)
            packet.addFloat(stuckPosZ, BITPACK_WORLD_POSITION_Z)
            packet.addBool(stuckMct)
            packet.addBool(stuckToObject)
            packet.addInt(stuckObjectId)
            if (stuckToObject) {
                packet.addFloat(stuckOffsetX, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffsetY, BITPACK_VEHICLE_VELOCITY)
                packet.addFloat(stuckOffsetZ, BITPACK_VEHICLE_VELOCITY)
                packet.addInt(stuckBone)
            }
            packet.addBool(stuckStaticAnim)
            if (stuckStaticAnim) {
                packet.addInt(stuckStaticAnimObjId)
            }
        }
    }
}
