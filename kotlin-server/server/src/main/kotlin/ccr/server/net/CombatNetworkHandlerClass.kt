package ccr.server.net

// C++: CombatNetworkHandlerClass (combat.h)
interface CombatNetworkHandlerClass {
    fun canDamage(damager: ArmedGameObj?, victim: PhysicalGameObj?): Boolean
    fun getDamageFactor(damager: ArmedGameObj?, victim: PhysicalGameObj?): Float
    fun onSoldierKill(killer: SoldierGameObj, victim: SoldierGameObj)
    fun onSoldierDeath(soldier: SoldierGameObj)
    fun isGameplayPermitted(): Boolean
}
