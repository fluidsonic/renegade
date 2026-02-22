#pragma once

#include "global.h"

#include "gameobjobserver.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class BuildingGameObj;

////////////////////////////////////////////////////////////////
//
//	BuildingMonitorClass
//
////////////////////////////////////////////////////////////////
class BuildingMonitorClass : public GameObjObserverClass
{
public:

	////////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////////
	BuildingMonitorClass (void) :
		Building (NULL)					{} 
	~BuildingMonitorClass (void)		{} 

	////////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////////
	void				Set_Building (BuildingGameObj *building)	{ Building = building; }

	//
	//	From GameObjObeserverClass
	//
	const char *	Get_Name (void)	{ return "BuildingMonitorClass"; }
	void				Killed (GameObject *game_obj, GameObject *killer);
	void				Damaged (GameObject *game_obj, GameObject *damager, float amount );
	void				Custom (GameObject *game_obj, int type, int param, GameObject *sender);
	
	//
	//	Unused methods from the base class
	//
	void				Attach (GameObject *)		{}
	void				Detach (GameObject *)		{}
	void				Created (GameObject *)		{}
	void				Destroyed (GameObject *)	{}
	void				Sound_Heard (GameObject *, const CombatSound &)	{}
	void				Enemy_Seen (GameObject *, GameObject *)			{}
	void				Action_Complete (GameObject *, int, ActionCompleteReason)	{}
	void				Timer_Expired (GameObject *, int)					{}
	void				Animation_Complete (GameObject *, const char *)	{}
	void				Poked (GameObject *, GameObject *)					{}
	void				Entered (GameObject *, GameObject *)				{}
	void				Exited (GameObject *, GameObject *)					{}

private:

	////////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////////
	BuildingGameObj *	Building;
};
