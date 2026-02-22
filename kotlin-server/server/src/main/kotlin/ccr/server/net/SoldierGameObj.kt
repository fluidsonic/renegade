package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*

// C++: SoldierGameObj (soldier.cpp)
// Full hierarchy: NetworkObject → BaseGameObj → PhysicalGameObj → DamageableGameObj
//                             → ArmedGameObj → SmartGameObj → SoldierGameObj
// Weapon entry for the weapon bag: definition ID + total rounds.
data class WeaponEntry(val definitionId: Int, val totalRounds: Int)

class SoldierGameObj(
    definitionId: Int,
    controlOwner: Int,
    team: Int,
    modelName: String,
    animName: String = "",
    position: Vector3,
    facing: Float = 0f,
    health: Float = 100f,
    val weapons: List<WeaponEntry> = emptyList(),
) : SmartGameObj(
    definitionId  = definitionId,
    position      = position,
    facing        = facing,
    modelName     = modelName,
    animName      = animName,
    health        = health,
    controlOwner  = controlOwner,
    team          = team,
) {
    // C++: cGod stores reference to cPlayer so buildings can award money via playerData
    var playerData: Player? = null

    // C++: SoldierGameObj::Export_Rare — calls super then appends definitionId (soldier.cpp).
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)       // PhysicalGameObj: model, anim, host, player_type, hud_pokable
        packet.addInt(definitionId)    // soldier definition_id repeated
    }

    // C++: SoldierGameObj::Export_Occasional — calls super then weapon list (weaponbag.cpp).
    // WeaponBag::Export_Weapon_List writes (WeaponList.Count()-1) then for each weapon from
    // slot 1 onward: id(32) + totalRounds(32). Slot 0 is always the empty weapon (fists).
    // Count()-1 is the number of REAL weapon entries that follow (not count-1 in the traditional sense).
    // Client reads this value directly as the loop count (no +1 adjustment).
    // Value 0 = no real weapons follow. A real C++ soldier typically has 1-2 real weapons.
    override fun exportOccasional(packet: BitStream) {
        super.exportOccasional(packet) // DamageableGameObj: is_dead, health, shield
        // WeaponBag::Export_Weapon_List: writes (Count()-1) = number of real weapons,
        // then for each from slot 1 onward: id(32) + totalRounds(32).
        packet.addInt(weapons.size)
        for (w in weapons) {
            packet.addInt(w.definitionId)
            packet.addInt(w.totalRounds)
        }
    }

    // C++: SoldierGameObj::Export_Frequent — writes soldier-specific fields first, then calls super.
    // Non-vehicle path: in_vehicle=false, has_weapon=false, no weapon data, no velocity, no anim name,
    // no special damage. SmartGameObj::Export_Frequent (called via super) handles on_host_bone,
    // targeting, and control.
    override fun exportFrequent(packet: BitStream) {
        packet.addBool(false)                                  // in_vehicle
        val hasWeapon = weapons.isNotEmpty()
        packet.addBool(hasWeapon)                              // has_weapon
        if (hasWeapon) {
            // C++: current weapon id + total rounds (from WeaponBagClass::Export)
            packet.addInt(weapons[0].definitionId)
            packet.addInt(weapons[0].totalRounds)
        }
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)
        packet.addInt(0, BITPACK_HUMAN_STATE)                  // human_state (0 = UPRIGHT)
        packet.addInt(0, BITPACK_HUMAN_SUB_STATE)              // human_sub_state
        // state != AIRBORNE → no velocity; state != TRANSITION/ANIMATION → no anim name
        packet.addBool(false)                                  // is_special_damage
        super.exportFrequent(packet)   // SmartGameObj → ArmedGameObj → PhysicalGameObj
    }
}
