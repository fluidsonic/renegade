#include "stdafx.h"
#include "pathfindstartdefinition.h"
#include "simpledefinitionfactory.h"
#include "definitionclassids.h"
#include "definitionmgr.h"
#include "persistfactory.h"
#include "editorchunkids.h"
#include "pathfindstartnode.h"


//////////////////////////////////////////////////////////////////////////////////
//	Constants
//////////////////////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_VARIABLES			= 0x00000100,
	CHUNKID_BASE_CLASS		= 0x00000200,
};


enum
{
	VARID_GAME_OBJ_ID			= 0x01
};


//////////////////////////////////////////////////////////////////////////////////
//
//	Static factories
//
//////////////////////////////////////////////////////////////////////////////////
DECLARE_DEFINITION_FACTORY(PathfindStartDefinitionClass, CLASSID_PATHFIND_START_DEF, "Pathfind Generator")	_PathfindStartDefFactory;
SimplePersistFactoryClass<PathfindStartDefinitionClass, CHUNKID_PATHFIND_START_DEF>									_PathfindStartPersistFactory;


//////////////////////////////////////////////////////////////////////////////////
//
//	PathfindStartDefinitionClass
//
//////////////////////////////////////////////////////////////////////////////////
PathfindStartDefinitionClass::PathfindStartDefinitionClass (void)
	:	m_GameObjectID (0),
		DefinitionClass ()
		
{	
	EDITABLE_PARAM( PathfindStartDefinitionClass, ParameterClass::TYPE_GAMEOBJDEFINITIONID, m_GameObjectID );
	return ;
}


//////////////////////////////////////////////////////////////////////////////////
//
//	~PathfindStartDefinitionClass
//
//////////////////////////////////////////////////////////////////////////////////
PathfindStartDefinitionClass::~PathfindStartDefinitionClass (void)
{
	return ;
}


//////////////////////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
//////////////////////////////////////////////////////////////////////////////////
const PersistFactoryClass &
PathfindStartDefinitionClass::Get_Factory (void) const
{
	return _PathfindStartPersistFactory;
}


//////////////////////////////////////////////////////////////////////////////////
//
//	Save
//
//////////////////////////////////////////////////////////////////////////////////
bool
PathfindStartDefinitionClass::Save (ChunkSaveClass &csave)
{
	bool retval = true;

	csave.Begin_Chunk (CHUNKID_BASE_CLASS);
		retval &= DefinitionClass::Save (csave);
	csave.End_Chunk ();	

	csave.Begin_Chunk (CHUNKID_VARIABLES);
		WRITE_MICRO_CHUNK (csave, VARID_GAME_OBJ_ID, m_GameObjectID);
	csave.End_Chunk ();

	return retval;
}


//////////////////////////////////////////////////////////////////////////////////
//
//	Load
//
//////////////////////////////////////////////////////////////////////////////////
bool
PathfindStartDefinitionClass::Load (ChunkLoadClass &cload)
{
	bool retval = true;

	while (cload.Open_Chunk ()) {
		switch (cload.Cur_Chunk_ID ()) {
			
			case CHUNKID_BASE_CLASS:
				retval &= DefinitionClass::Load (cload);
				break;

			case CHUNKID_VARIABLES:
				retval &= Load_Variables (cload);
				break;
		}

		cload.Close_Chunk ();
	}

	return retval;
}


///////////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
///////////////////////////////////////////////////////////////////////
bool
PathfindStartDefinitionClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {
			
			READ_MICRO_CHUNK (cload, VARID_GAME_OBJ_ID, m_GameObjectID);
		}

		cload.Close_Micro_Chunk ();
	}

	return true;
}


//////////////////////////////////////////////////////////////////////////////////
//
//	Create
//
//////////////////////////////////////////////////////////////////////////////////
PersistClass *
PathfindStartDefinitionClass::Create (void) const
{
	return new PathfindStartNodeClass ();
}

