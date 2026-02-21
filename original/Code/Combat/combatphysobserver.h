#ifndef	COMBATPHYSOBSERVER_H
#define	COMBATPHYSOBSERVER_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	PHYSOBSERVER_H
	#include "physobserver.h"
#endif

class	DamageableGameObj;
class	PhysicalGameObj;
class BuildingGameObj;

class	CombatPhysObserverClass : public PhysObserverClass {

public:
	virtual	DamageableGameObj *	As_DamageableGameObj( void )	{ return NULL; }
	virtual	PhysicalGameObj *		As_PhysicalGameObj( void )		{ return NULL; }
	virtual	BuildingGameObj *		As_BuildingGameObj( void )		{ return NULL; }
};

#endif	//	COMBATPHYSOBSERVER_H
