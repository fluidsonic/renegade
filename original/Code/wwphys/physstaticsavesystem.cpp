#include "physstaticsavesystem.h"
#include "wwphysids.h"
#include "pscene.h"
#include "pathfind.h"
#include "chunkio.h"
#include "saveload.h"
#include "rendobj.h"
#include "phys.h"

/*
** Instantiate the Physics Static-Data-Save-Systems
*/
PhysStaticDataSaveSystemClass			_PhysStaticDataSaveSystem;
PhysStaticObjectsSaveSystemClass		_PhysStaticObjectsSaveSystem;


/**************************************************************************************
**
** PhysStaticDataSaveSystemClass Implementation
**
**************************************************************************************/
uint32 PhysStaticDataSaveSystemClass::Chunk_ID(void) const
{
	return PHYSICS_CHUNKID_STATIC_DATA_SUBSYSTEM;
}

bool PhysStaticDataSaveSystemClass::Save(ChunkSaveClass &csave)
{	

	csave.Begin_Chunk(PSDSSC_CHUNKID_SCENE);
	PhysicsSceneClass::Get_Instance()->Save_Level_Static_Data(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(PSDSSC_CHUNKID_PATHFIND);
	PathfindClass::Get_Instance()->Save(csave);
	csave.End_Chunk();
	return true;
}

bool PhysStaticDataSaveSystemClass::Load(ChunkLoadClass &cload)
{

	while (cload.Open_Chunk()) {
		switch (cload.Cur_Chunk_ID()) 
		{
			case PSDSSC_CHUNKID_SCENE:
				PhysicsSceneClass::Get_Instance()->Load_Level_Static_Data(cload);
				break;

			case PSDSSC_CHUNKID_PATHFIND:
				PathfindClass::Get_Instance()->Load(cload);
				break;
		}
		cload.Close_Chunk();
	}
	SaveLoadSystemClass::Register_Post_Load_Callback(this);
	return true;
}

void PhysStaticDataSaveSystemClass::On_Post_Load(void)
{
	PhysicsSceneClass::Get_Instance()->Post_Load_Level_Static_Data();
	//PathfindClass::Get_Instance()->On_Post_Load();
}


/**************************************************************************************
**
** PhysStaticObjectsSaveSystemClass Implementation
**
**************************************************************************************/
uint32 PhysStaticObjectsSaveSystemClass::Chunk_ID(void) const
{
	return PHYSICS_CHUNKID_STATIC_OBJECTS_SUBSYSTEM;
}

bool PhysStaticObjectsSaveSystemClass::Save(ChunkSaveClass &csave)
{	
	csave.Begin_Chunk(PSOSSC_CHUNKID_SCENE);
	PhysicsSceneClass::Get_Instance()->Save_Level_Static_Objects(csave);
	csave.End_Chunk();

	return true;
}

bool PhysStaticObjectsSaveSystemClass::Load(ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {
		switch (cload.Cur_Chunk_ID()) 
		{
			case PSOSSC_CHUNKID_SCENE:
				PhysicsSceneClass::Get_Instance()->Load_Level_Static_Objects(cload);
				break;

		}
		cload.Close_Chunk();
	}
	return true;
}

void PhysStaticObjectsSaveSystemClass::On_Post_Load(void)
{
	PhysicsSceneClass::Get_Instance()->Post_Load_Level_Static_Objects();
}

