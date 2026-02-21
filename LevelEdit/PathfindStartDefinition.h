#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __PATHFIND_START_DEFINITION_H
#define __PATHFIND_START_DEFINITION_H

#include "definition.h"
#include "editorchunkids.h"


//////////////////////////////////////////////////////////////////////////////////
//
//	PathfindStartDefinitionClass
//
//////////////////////////////////////////////////////////////////////////////////
class PathfindStartDefinitionClass : public DefinitionClass
{

public:

	/////////////////////////////////////////////////////////////////////
	//	Editable interface requirements
	/////////////////////////////////////////////////////////////////////
	DECLARE_EDITABLE(PathfindStartDefinitionClass, DefinitionClass);

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	PathfindStartDefinitionClass (void);
	virtual ~PathfindStartDefinitionClass (void);

	// From DefinitionClass
	virtual uint32								Get_Class_ID (void) const { return CLASSID_PATHFIND_START_DEF; }

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual PersistClass *					Create (void) const;

	// PathfindStartDefinitionClass specific
	int											Get_Object_Type (void) const { return m_GameObjectID; }

protected:
	
	/////////////////////////////////////////////////////////////////////
	//	Protected methods
	/////////////////////////////////////////////////////////////////////
	bool											Load_Variables (ChunkLoadClass &cload);

private:

	/////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////
	int m_GameObjectID;
};



#endif //__PATHFIND_START_DEFINITION_H

