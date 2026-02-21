#include "buildingmonitor.h"
#include "building.h"


////////////////////////////////////////////////////////////////
//
//	Killed
//
////////////////////////////////////////////////////////////////
void
BuildingMonitorClass::Killed (GameObject *game_obj, GameObject *killer)
{
	WWASSERT (Building != NULL);

	//
	//	Notify the building
	//
	Building->On_Destroyed ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Damaged
//
////////////////////////////////////////////////////////////////
void
BuildingMonitorClass::Damaged (GameObject *game_obj, GameObject *killer, float amount )
{
	// Only report damage not heal
	if (amount > 0.0f) {
		WWASSERT (Building != NULL);

		// If the building has been damaged to the point of being destroyed
		// then do not report the damaged event. A killed event will be sent
		// later.
		DefenseObjectClass* defense = Building->Get_Defense_Object();

		if (defense && (defense->Get_Health() <= 0.0f)) {
			return;
		}

		DamageableGameObj* damager = NULL;

		if (killer) {
			damager = killer->As_DamageableGameObj();
		}

		bool friendlyFire = (damager && (damager->Get_Player_Type() == Building->Get_Player_Type()));

		//	Notify the building
		if (!friendlyFire) {
			Building->On_Damaged();
		}
	}
}


////////////////////////////////////////////////////////////////
//
//	Custom
//
////////////////////////////////////////////////////////////////
void
BuildingMonitorClass::Custom (GameObject *game_obj, int type, int param, GameObject *sender)
{
	WWASSERT (Building != NULL);

	//
	//	Notify the controller
	//
	if (type == CUSTOM_EVENT_BUILDING_POWER_CHANGED) {
		//Building->On_Building_Damaged (game_obj->As_BuldingGameObj ());
	}

	return ;
}
