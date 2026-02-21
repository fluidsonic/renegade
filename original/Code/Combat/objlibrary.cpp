#include "objlibrary.h"
#include "definition.h"
#include "definitionfactory.h"
#include "definitionmgr.h"

/*
**
*/
PhysicalGameObj	*ObjectLibraryManager::Create_Object( int def_id )
{
	DefinitionClass * def = DefinitionMgrClass::Find_Definition( def_id );
	StringClass error_message;
	if ((def != NULL) && (CLASSID_GAME_OBJECTS == SuperClassID_From_ClassID(def->Get_Class_ID()))) {
		if (def->Is_Valid_Config(error_message)) {
			return (PhysicalGameObj *)def->Create();
		} else {
			return NULL;
		}
	}
	return NULL;
}

PhysicalGameObj	*ObjectLibraryManager::Create_Object( const char *name )
{
	DefinitionClass * def = DefinitionMgrClass::Find_Typed_Definition( name, CLASSID_GAME_OBJECTS );
	StringClass error_message;
//	if ( def && CLASSID_GAME_OBJECTS == SuperClassID_From_ClassID(def->Get_Class_ID())) {
	if ( def ) {
		if (def->Is_Valid_Config(error_message)) {
			return (PhysicalGameObj *)def->Create();
		} else {
			return NULL;
		}
	} else if ( def ) {
	}

	return NULL;
}

