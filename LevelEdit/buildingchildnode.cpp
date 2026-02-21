#include "stdafx.h"
#include "buildingnode.h"
#include "buildingchildnode.h"
#include "sceneeditor.h"
#include "collisiongroups.h"
#include "persistfactory.h"
#include "editorchunkids.h"
#include "preset.h"
#include "chunkio.h"
#include "nodemgr.h"
#include "modelutils.h"


//////////////////////////////////////////////////////////////////////////////
//	Persist factory
//////////////////////////////////////////////////////////////////////////////
SimplePersistFactoryClass<BuildingChildNodeClass, CHUNKID_NODE_BUILDING_CHILD> _BuildingChildNodePersistFactory;


enum
{
	CHUNKID_VARIABLES			= 0x05260946,
	CHUNKID_BASE_CLASS
};

enum
{
	VARID_REQUIRES_CROUCH	= 0x01,
	VARID_ATTACK_POINT,
};


//////////////////////////////////////////////////////////////////////////////
//
//	BuildingChildNodeClass
//
//////////////////////////////////////////////////////////////////////////////
BuildingChildNodeClass::BuildingChildNodeClass (PresetClass *preset)
	:	PhysObj (NULL),
		Building (NULL),
		NodeClass (preset)		
{
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	BuildingChildNodeClass
//
//////////////////////////////////////////////////////////////////////////////
BuildingChildNodeClass::BuildingChildNodeClass (const BuildingChildNodeClass &src)
	:	PhysObj (NULL),
		Building (NULL),
		NodeClass (NULL)
{
	*this = src;
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	~BuildingChildNodeClass
//
//////////////////////////////////////////////////////////////////////////////
BuildingChildNodeClass::~BuildingChildNodeClass (void)
{	
	Remove_From_Scene ();
	MEMBER_RELEASE (PhysObj);

	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	Initialize
//
//	Note:  This may be called more than once.  It is used as an 'initialize'
// and a 're-initialize'.
//
//////////////////////////////////////////////////////////////////////////////
void
BuildingChildNodeClass::Initialize (void)
{
	MEMBER_RELEASE (PhysObj);

	//
	//	Create the attack-point render object
	//
	RenderObjClass *render_obj = ::Create_Render_Obj ("CARMARKER");
	WWASSERT (render_obj != NULL);
	if (render_obj != NULL) {
		
		// Create the new physics object
		PhysObj = new DecorationPhysClass;
		
		//
		// Configure the physics object with information about
		// its new render object and collision data.
		//
		PhysObj->Set_Model (render_obj);
		PhysObj->Set_Transform (Matrix3D (1));
		PhysObj->Set_Collision_Group (EDITOR_COLLISION_GROUP);
		PhysObj->Peek_Model ()->Set_User_Data ((PVOID)&m_HitTestInfo, FALSE);
		PhysObj->Set_Transform (m_Transform);
		::Set_Model_Collision_Type (PhysObj->Peek_Model (), COLLISION_TYPE_0);
		
		// Release our hold on the render object pointer
		MEMBER_RELEASE (render_obj);
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
////////////////////////////////////////////////////////////////
const PersistFactoryClass &
BuildingChildNodeClass::Get_Factory (void) const
{	
	return _BuildingChildNodePersistFactory;
}


/////////////////////////////////////////////////////////////////
//
//	Save
//
/////////////////////////////////////////////////////////////////
bool
BuildingChildNodeClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_BASE_CLASS);
		NodeClass::Save (csave);
	csave.End_Chunk ();

	csave.Begin_Chunk (CHUNKID_VARIABLES);
	csave.End_Chunk ();
	return true;
}


/////////////////////////////////////////////////////////////////
//
//	Load
//
/////////////////////////////////////////////////////////////////
bool
BuildingChildNodeClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {		
		switch (cload.Cur_Chunk_ID ()) {

			case CHUNKID_BASE_CLASS:
				NodeClass::Load (cload);
				break;
			
			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;
		}

		cload.Close_Chunk ();
	}

	return true;
}


///////////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
///////////////////////////////////////////////////////////////////////
bool
BuildingChildNodeClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		/*switch (cload.Cur_Micro_Chunk_ID ()) {			
		}*/

		cload.Close_Micro_Chunk ();
	}

	return true;
}


/////////////////////////////////////////////////////////////////
//
//	operator=
//
/////////////////////////////////////////////////////////////////
const BuildingChildNodeClass &
BuildingChildNodeClass::operator= (const BuildingChildNodeClass &src)
{
	NodeClass::operator= (src);
	return *this;
}


//////////////////////////////////////////////////////////////////////
//
//	Pre_Export
//
//////////////////////////////////////////////////////////////////////
void
BuildingChildNodeClass::Pre_Export (void)
{
	//
	//	Remove ourselves from the 'system' so we don't get accidentally
	// saved during the export. 
	//
	Add_Ref ();
	if (PhysObj != NULL && m_IsInScene) {
		::Get_Scene_Editor ()->Remove_Object (PhysObj);
	}

	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Post_Export
//
//////////////////////////////////////////////////////////////////////
void
BuildingChildNodeClass::Post_Export (void)
{
	//
	//	Put ourselves back into the system
	//
	if (PhysObj != NULL && m_IsInScene) {
		::Get_Scene_Editor ()->Add_Dynamic_Object (PhysObj);
	}

	Release_Ref ();
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	On_Delete
//
//////////////////////////////////////////////////////////////////////
void
BuildingChildNodeClass::On_Delete (void)
{
	//
	//	Remove ourselves from the cover spot
	//
	if (Building) {
		Building->Remove_Child_Node (this);
	}

	return ;
}

