#if defined(_MSV_VER)
#pragma once
#endif

#ifndef NETHANDLER_H
#define NETHANDLER_H

#ifndef ALWAYS_H
	#include "always.h"
#endif

#ifndef COMBAT_H
   #include "combat.h"
#endif

//
// The combat lib calls these commando application-level methods.
// Other applications using the combat lib will not implement a class 
// like this one.
//

class	GameCombatNetworkHandlerClass : public CombatNetworkHandlerClass
{
public:
	bool	Can_Damage(ArmedGameObj * p_armed_damager, PhysicalGameObj * p_phys_victim);
	float	Get_Damage_Factor(ArmedGameObj * p_armed_damager, PhysicalGameObj * p_phys_victim);
	bool	Is_Gameplay_Permitted(void);
	void	On_Soldier_Kill(SoldierGameObj * p_soldier, SoldierGameObj * p_victim);
	void	On_Soldier_Death(SoldierGameObj * p_soldier);
};

#endif // NETHANDLER_H


