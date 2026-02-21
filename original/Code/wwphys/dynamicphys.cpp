#include "dynamicphys.h"
#include "chunkio.h"
#include "pscene.h"
#include "physcoltest.h"

#if (UMBRASUPPORT)
#include <umbra.hpp>
#endif

#include "umbrasupport.h"

/***********************************************************************************************
**
** DynamicPhysClass Implementation
** Note that this is not a concrete class.  It therefore does not have factories defined...
**
***********************************************************************************************/

const int MIN_VIS_UPDATE_TICK_DELAY = 250;  // min number of milliseconds between dynamic vis id updates

bool DynamicPhysClass::_DisableDynamicPhysSimulation		= false;
bool DynamicPhysClass::_DisableDynamicPhysRendering		= false;

/*
** Chunk ID's used by DynamicPhysClass
*/
enum
{
	DYNAMICPHYS_CHUNK_PHYS			= 813001100,
};

DynamicPhysClass::DynamicPhysClass(void) :
	DirtyVisObjectID(true),
	VisNodeID(0),
	VisStatusLastUpdated(0)
{
}

DynamicPhysClass::~DynamicPhysClass(void)
{
}

void DynamicPhysClass::Init(const DynamicPhysDefClass & definition)
{
	PhysClass::Init(definition);	
}

void DynamicPhysClass::Set_Model(RenderObjClass * model)
{
	PhysClass::Set_Model(model);

#if (UMBRASUPPORT)
	if (model != NULL) {
		/*
		** Create a new test-model for the bounding box of this object
		*/
		AABoxClass obj_box;
		model->Get_Obj_Space_Bounding_Box(obj_box);
		
		/*
		** Insert it into our Umbra object
		*/
		UmbraObject->setTestModel(UmbraSupport::Create_Box_Model(obj_box));
		UmbraObject->setCost(100000,100000,5);
	}
#endif
}

void DynamicPhysClass::Update_Visibility_Status(void)
{
	/*
	** Invalidate our cached vis object ID
	*/
	DirtyVisObjectID = true;

	/*
	** Invalidate the lighting cache.  Next time this object is rendered the cache will be updated.
	*/
	Invalidate_Static_Lighting_Cache();
}

int DynamicPhysClass::Get_Vis_Object_ID(void)
{
	if (DirtyVisObjectID) {
		Internal_Update_Visibility_Status();
	}
	return VisObjectID;
}

void DynamicPhysClass::Internal_Update_Visibility_Status(void)
{
	/*
	** Don't update our VIS-ID more often than 4 times per second
	*/
	unsigned current_time=WW3D::Get_Sync_Time();
	unsigned delta = current_time - VisStatusLastUpdated;

	if (delta < MIN_VIS_UPDATE_TICK_DELAY) return;
	VisStatusLastUpdated=current_time;

	/*
	** Update our VIS-ID
	*/
	VisObjectID = PhysicsSceneClass::Get_Instance()->Get_Dynamic_Object_Vis_ID(Model->Get_Bounding_Box(),&VisNodeID);
	if ((int)VisObjectID >= PhysicsSceneClass::Get_Instance()->Get_Vis_Table_Size()) {
		
		int size = PhysicsSceneClass::Get_Instance()->Get_Vis_Table_Size();
//		int id = PhysicsSceneClass::Get_Instance()->Get_Dynamic_Object_Vis_ID(Model->Get_Bounding_Box(),&VisNodeID);

		VisObjectID = 0;
	}

	/*
	** Clear the dirty bit
	*/
	DirtyVisObjectID = false;

	/*
	** Update our Umbra Object
	*/
#if (UMBRASUPPORT)
	UmbraSupport::Update_Umbra_Object(this);
#endif

}

bool DynamicPhysClass::Save(ChunkSaveClass &csave)
{
	csave.Begin_Chunk(DYNAMICPHYS_CHUNK_PHYS);
	PhysClass::Save(csave);
	csave.End_Chunk();
	return true;
}

bool DynamicPhysClass::Load(ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {
		
		switch(cload.Cur_Chunk_ID()) 
		{
			case DYNAMICPHYS_CHUNK_PHYS:
				PhysClass::Load(cload);
				break;

			default:
				break;
		}
		
		cload.Close_Chunk();
	}
	SaveLoadSystemClass::Register_Post_Load_Callback(this);
	return true;
}

void DynamicPhysClass::On_Post_Load(void)
{
	PhysClass::On_Post_Load();
	
	// update cached vis object id, vis node id, and sunlight status...
	Update_Cull_Box();
	Update_Visibility_Status();
}

/***********************************************************************************************
**
** DynamicPhysDefClass Implementation
** This holds the description for a DynamicPhysClass.  Again, this class isn't concrete
** so it doesn't have factories...
**
***********************************************************************************************/

enum 
{
	DYNAMICPHYSDEF_CHUNK_PHYSDEF	= 813001104,			// parent class data.
};

DynamicPhysDefClass::DynamicPhysDefClass(void)
{
}

bool DynamicPhysDefClass::Is_Valid_Config(StringClass &message)
{
	return PhysDefClass::Is_Valid_Config(message);
}

bool DynamicPhysDefClass::Is_Type(const char * type_name)
{
	if (stricmp(type_name,DynamicPhysDefClass::Get_Type_Name()) == 0) {
		return true;
	} else {
		return PhysDefClass::Is_Type(type_name);
	}
}

bool DynamicPhysDefClass::Save(ChunkSaveClass &csave)
{
	csave.Begin_Chunk(DYNAMICPHYSDEF_CHUNK_PHYSDEF);
	PhysDefClass::Save(csave);
	csave.End_Chunk();
	return true;
}

bool DynamicPhysDefClass::Load(ChunkLoadClass &cload)
{
	while (cload.Open_Chunk()) {

		switch(cload.Cur_Chunk_ID()) {			

			case DYNAMICPHYSDEF_CHUNK_PHYSDEF:
				PhysDefClass::Load(cload);
				break;
		}

		cload.Close_Chunk();
	}
	return true;
}

