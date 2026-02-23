package ccr.server.defs

// C++: ArmedGameObjDef : public PhysicalGameObjDef (armedgameobj.h)
open class ArmedGameObjDef(
    name: String,
    id: UInt,
    chunkId: UInt,
    val weaponDefId: Int = 0,             // C++: int WeaponDefID
    val secondaryWeaponDefId: Int = 0,    // C++: int SecondaryWeaponDefID
    val weaponRounds: Int = -1,           // C++: safe_int WeaponRounds
    val weaponTiltRate: Float = 1.0f,     // C++: float WeaponTiltRate
    val weaponTiltMin: Float = -10000f,   // C++: float WeaponTiltMin
    val weaponTiltMax: Float = 10000f,    // C++: float WeaponTiltMax
    val weaponTurnRate: Float = 1.0f,     // C++: float WeaponTurnRate
    val weaponTurnMin: Float = -10000f,   // C++: float WeaponTurnMin
    val weaponTurnMax: Float = 10000f,    // C++: float WeaponTurnMax
    val weaponError: Float = 0f,          // C++: float WeaponError
) : PhysicalGameObjDef(name, id, chunkId)
