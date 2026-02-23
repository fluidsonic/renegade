package ccr.server.net

import ccr.math.Vector3

// C++: CombatSoundType enum (combatsound.h)
enum class CombatSoundType(val value: Int) {
    NONE(0),
    OLD_WEAPON(1),
    FOOTSTEPS(2),
    VEHICLE(3),
    GUNSHOT(4),
    BULLET_HIT(5),
    DESIGNER01(991),
    DESIGNER02(992),
    DESIGNER03(993),
    DESIGNER04(994),
    DESIGNER05(995),
    DESIGNER06(996),
    DESIGNER07(997),
    DESIGNER08(998),
    DESIGNER09(999),
}

// C++: CombatSound (combatsound.h)
class CombatSound {
    var position: Vector3 = Vector3()          // C++: Vector3 Position
    var type: CombatSoundType = CombatSoundType.NONE  // C++: CombatSoundType Type
    var creator: ScriptableGameObj? = null     // C++: ScriptableGameObj* Creator
}
