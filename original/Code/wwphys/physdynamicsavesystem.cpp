#include "global.h"
#include "physdynamicsavesystem.h"
#include "wwphysids.h"
#include "pscene.h"
#include "physcon.h"
#include "chunkio.h"
#include "pathmgr.h"
#include "saveload.h"
#include "rendobj.h"
#include "phys.h"

/*
** Instantiate the Physics Dynamic-Data-Save-System
*/
PhysDynamicSaveSystemClass _PhysDynamicSaveSystem;

uint32_t PhysDynamicSaveSystemClass::Chunk_ID(void) const
{
	return PHYSICS_CHUNKID_DYNAMIC_DATA_SUBSYSTEM;
}

bool PhysDynamicSaveSystemClass::Contains_Data(void) const
{
	// TODO: could check if we have anything to save...
	return true;
}

bool PhysDynamicSaveSystemClass::Save(ChunkSaveClass &csave)
{	

	csave.Begin_Chunk(PDSSC_CHUNKID_SCENE);
	PhysicsSceneClass::Get_Instance()->Save_Level_Dynamic_Data(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(PDSSC_CHUNKID_CONSTANTS);
	PhysicsConstants::Save(csave);
	csave.End_Chunk();

	csave.Begin_Chunk(PDSSC_CHUNKID_PATHMGR);
	PathMgrClass::Save(csave);
	csave.End_Chunk();

	return true;
}

bool PhysDynamicSaveSystemClass::Load(ChunkLoadClass &cload)
{

	while (cload.Open_Chunk()) {
		switch (cload.Cur_Chunk_ID()) {
		case PDSSC_CHUNKID_SCENE:
			PhysicsSceneClass::Get_Instance()->Load_Level_Dynamic_Data(cload);
			break;
		case PDSSC_CHUNKID_CONSTANTS:
			PhysicsConstants::Load(cload);
			break;
		case PDSSC_CHUNKID_PATHMGR:
			PathMgrClass::Load(cload);
			break;
		}
		cload.Close_Chunk();
	}
	
	SaveLoadSystemClass::Register_Post_Load_Callback(this);
	return true;
}

void PhysDynamicSaveSystemClass::On_Post_Load(void)
{
	PhysicsSceneClass::Get_Instance()->Post_Load_Level_Dynamic_Data();
}
