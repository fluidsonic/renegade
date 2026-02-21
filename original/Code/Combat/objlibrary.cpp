#include "objlibrary.h"
#include "debug.h"
#include "wwhack.h"
#include "wwdebug.h"
#include "wwmemlog.h"
#include "definition.h"
#include "definitionfactory.h"
#include "definitionmgr.h"

/*
**
*/
PhysicalGameObj	*ObjectLibraryManager::Create_Object( int def_id )
{
	WWMEMLOG(MEM_GAMEDATA);
	DefinitionClass * def = DefinitionMgrClass::Find_Definition( def_id );
	StringClass error_message;
	if ((def != NULL) && (CLASSID_GAME_OBJECTS == SuperClassID_From_ClassID(def->Get_Class_ID()))) {
		if (def->Is_Valid_Config(error_message)) {
			return (PhysicalGameObj *)def->Create();
		} else {
			WWDEBUG_SAY(("Could not create object %s!\n%s\n",def->Get_Name(),error_message));
			return NULL;
		}
	}
	WWDEBUG_SAY(( "Didn't find Definition of (%d)\n", def_id ));
	return NULL;
}

PhysicalGameObj	*ObjectLibraryManager::Create_Object( const char *name )
{
	WWMEMLOG(MEM_GAMEDATA);
	DefinitionClass * def = DefinitionMgrClass::Find_Typed_Definition( name, CLASSID_GAME_OBJECTS );
	StringClass error_message;
//	if ( def && CLASSID_GAME_OBJECTS == SuperClassID_From_ClassID(def->Get_Class_ID())) {
	if ( def ) {
		if (def->Is_Valid_Config(error_message)) {
			return (PhysicalGameObj *)def->Create();
		} else {
			WWDEBUG_SAY(("Could not create object %s!\n%s\n",def->Get_Name(),error_message));
			return NULL;
		}
	} else if ( def ) {
		WWDEBUG_SAY(( "Found Definition for %s, but is was the wrong type\n", name ));
	}

	WWDEBUG_SAY(( "Didn't find Definition of \"%s\"\n", name ));
	return NULL;
}


/*
**
*/
void Force_Link_Combat( void )
{
	FORCE_LINK( C4 );
	FORCE_LINK( PowerUp );
	FORCE_LINK( SAMSite );
	FORCE_LINK( Simple );
	FORCE_LINK( Soldier );
	FORCE_LINK( Vehicle );
	FORCE_LINK( Zone );
	FORCE_LINK( DamageZone );	
	FORCE_LINK( Transition );
	FORCE_LINK( Cinematic );
	FORCE_LINK( SpecialEffects );
	FORCE_LINK( SakuraBoss );
	FORCE_LINK( MendozaBoss );
	FORCE_LINK( RaveshawBoss );
	FORCE_LINK( Beacon );

	FORCE_LINK( PowerPlant );
	FORCE_LINK( SoldierFactory );
	FORCE_LINK( VehicleFactory );
	FORCE_LINK( Refinery );
	FORCE_LINK( ComCenter );
	FORCE_LINK( RepairBay );
	FORCE_LINK( AirStrip );
	FORCE_LINK( WarFactory );	
	
	FORCE_LINK( doorphys );
	FORCE_LINK( elevatorphys );
	FORCE_LINK( damageablestaticphys );
	FORCE_LINK( buildingaggregate );

	FORCE_LINK( GlobalSettings );
	FORCE_LINK( EvaSettings );
	FORCE_LINK( CharClassSettings );	
	FORCE_LINK( OratorTypes );
	FORCE_LINK( PurchaseSettings );
	FORCE_LINK( TeamPurchaseSettings );	
}

