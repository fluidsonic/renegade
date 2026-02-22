#pragma once

#include "global.h"


#include "physobserver.h"

class	DamageableGameObj;
class	PhysicalGameObj;
class BuildingGameObj;

class	CombatPhysObserverClass : public PhysObserverClass {

public:
	virtual	DamageableGameObj *	As_DamageableGameObj( void )	{ return NULL; }
	virtual	PhysicalGameObj *		As_PhysicalGameObj( void )		{ return NULL; }
	virtual	BuildingGameObj *		As_BuildingGameObj( void )		{ return NULL; }
};
