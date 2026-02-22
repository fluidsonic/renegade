#pragma once

#include "global.h"

#include "definitionclassids.h"

class DefinitionFactoryClass;

//////////////////////////////////////////////////////////////////////////////////
//
//	DefinitionFactoryMgrClass 
//
//////////////////////////////////////////////////////////////////////////////////
class DefinitionFactoryMgrClass
{
public:

	/////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////
	static DefinitionFactoryClass *	Find_Factory (uint32_t class_id);	
	static DefinitionFactoryClass *	Find_Factory (const char *name);
	static void								Register_Factory (DefinitionFactoryClass *factory);
	static void								Unregister_Factory (DefinitionFactoryClass *factory);

	// Class enumeration
	static DefinitionFactoryClass *	Get_First (uint32_t superclass_id);
	static DefinitionFactoryClass *	Get_Next (DefinitionFactoryClass *current, uint32_t superclass_id);

	// Factory enumeration
	static DefinitionFactoryClass *	Get_First (void);
	static DefinitionFactoryClass *	Get_Next (DefinitionFactoryClass *current);
	
private:

	/////////////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////////////
	static void								Link_Factory (DefinitionFactoryClass *factory);
	static void								Unlink_Factory (DefinitionFactoryClass *factory);

	/////////////////////////////////////////////////////////////////////
	//	Static member data
	/////////////////////////////////////////////////////////////////////
	static DefinitionFactoryClass *	_FactoryListHead;
};
