#include "combatdazzle.h"
#include "rinfo.h"
#include "camera.h"
#include "pscene.h"
#include "physcoltest.h"
#include "phys.h"
#include "combat.h"
#include "soldier.h"


CombatDazzleClass _TheCombatDazzleHandler;


/***********************************************************************************************
 * CombatDazzleClass::Compute_Dazzle_Visibility -- Computes visibility of a dazzle object in t *
 *                                                                                             *
 *    This dazzle visibility handler is used for the background and game scene in renegade.    *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   6/13/2001  gth : Created.                                                                 *
 *=============================================================================================*/
float CombatDazzleClass::Compute_Dazzle_Visibility
(
	RenderInfoClass & rinfo,
	DazzleRenderObjClass * obj,	
	const Vector3 & point
) const
{
	/*
	** If we are in first-person mode, don't collide against the star
	*/
	bool ignore_player = CombatManager::Is_First_Person();
	if (ignore_player) {
		if (COMBAT_STAR != NULL) {
			COMBAT_STAR->Peek_Physical_Object()->Inc_Ignore_Counter();
		}
	}
	
	/*
	** Cast a ray from the camera to the dazzle position
	*/
	CastResultStruct res;
	LineSegClass ray(rinfo.Camera.Get_Position(),point);
	PhysRayCollisionTestClass raytest(ray,&res,0,COLLISION_TYPE_PROJECTILE);

	PhysicsSceneClass * scene = PhysicsSceneClass::Get_Instance();
	if (scene != NULL) {
		scene->Cast_Ray(raytest);
	}

	/*
	** Done
	*/
	if (ignore_player) {
		if (COMBAT_STAR != NULL) {
			COMBAT_STAR->Peek_Physical_Object()->Dec_Ignore_Counter();
		}
	}

	if (res.Fraction == 1.0f) {
		return 1.0f;
	} else {
		return 0.0f;
	}
}


