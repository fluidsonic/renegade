#ifndef COMBATDAZZLE_H
#define COMBATDAZZLE_H

#include "always.h"
#include "dazzle.h"

/**
** CombatDazzleClass - this class handles the visibility callback for the combat scene.
** This handler should be installed while the background scene and the game scene
** are rendered and it determines visibility by asking the game scene to cast a ray
** from the camera to the dazzle position.
*/
class CombatDazzleClass : public DazzleVisibilityClass
{
	float Compute_Dazzle_Visibility(	RenderInfoClass & rinfo,
												DazzleRenderObjClass * dazzle,
												const Vector3 & point) const;
};

extern CombatDazzleClass _TheCombatDazzleHandler;

#endif

