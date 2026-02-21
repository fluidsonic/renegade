
#ifndef COMBATMATERIALEFFECTMANAGER_H
#define COMBATMATERIALEFFECTMANAGER_H

#include "always.h"

class TransitionEffectClass;

class CombatMaterialEffectManager
{
public:
	static TransitionEffectClass * Get_Spawn_Effect(void);
	static TransitionEffectClass * Get_Death_Effect(void);
	static TransitionEffectClass * Get_Health_Effect(void);
	static TransitionEffectClass * Get_Electrocution_Effect(void);
};

#endif

