#pragma once

#include "global.h"

#include "networkobject.h"

class SoldierGameObj;

//-----------------------------------------------------------------------------
//
// Server computation of object priority for a given client
//
class	cPriority
{
public:
	static float			Compute_Object_Priority(int client_id, const Vector3 & client_pos, NetworkObjectClass * p_netobject, bool do_it_anyway = false, SoldierGameObj *client_soldier = NULL);
	static float			Get_Object_Distance(const Vector3 &	client_pos, NetworkObjectClass * p_netobject);

	static float			Compute_Object_Priority_2(int client_id, const Vector3 & client_pos, NetworkObjectClass * p_netobject, bool do_it_anyway = false, SoldierGameObj *client_soldier = NULL);
	static float			Get_Object_Distance_2(const Vector3 &	client_pos, NetworkObjectClass * p_netobject);

private:
	static float			Compute_Facing_Factor(int client_id, const Vector3 &	client_pos, NetworkObjectClass * p_netobject, SoldierGameObj *client_soldier = NULL);
	static float			Compute_Type_Factor(NetworkObjectClass * p_netobject);
	static float			Compute_Relevance_Factor(int client_id, NetworkObjectClass * p_netobject, SoldierGameObj *client_soldier = NULL);

	static float			Compute_Facing_Factor_2(int client_id, const Vector3 &	client_pos, NetworkObjectClass * p_netobject, SoldierGameObj *client_soldier = NULL);
	static float			Compute_Type_Factor_2(NetworkObjectClass * p_netobject, float distance);
	static float			Compute_Relevance_Factor_2(int client_id, NetworkObjectClass * p_netobject, SoldierGameObj *client_soldier = NULL);

	static float			MaxDistance;
	static const float	MAX_FACING_PENALTY;
	static const float	TURRET_FACTOR;
	static const float	VEHICLE_FACTOR;
	static const float	SOLDIER_FACTOR;
	static const float	SOLDIER_IN_VEHICLE_FACTOR;
	static const float	BUILDING_FACTOR;

};

//-----------------------------------------------------------------------------
