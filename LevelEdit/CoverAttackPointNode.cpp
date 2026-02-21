#include "stdafx.h"
#include "coverspotnode.h"
#include "coverattackpointnode.h"
#include "sceneeditor.h"
#include "collisiongroups.h"
#include "persistfactory.h"
#include "editorchunkids.h"
#include "preset.h"
#include "chunkio.h"
#include "nodemgr.h"
#include "modelutils.h"
#include "soldier.h"
#include "presetmgr.h"

//////////////////////////////////////////////////////////////////////////////
//	Persist factory
//////////////////////////////////////////////////////////////////////////////
SimplePersistFactoryClass<CoverAttackPointNodeClass, CHUNKID_NODE_COVER_ATTACK_POINT> _CoverAttackPointNodePersistFactory;


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
//	Static member initialization
//////////////////////////////////////////////////////////////////////////////
PhysClass *	CoverAttackPointNodeClass::_TheCollisionObj	= NULL;
int			CoverAttackPointNodeClass::_InstanceCount		= 0;


//////////////////////////////////////////////////////////////////////////////
//
//	CoverAttackPointNodeClass
//
//////////////////////////////////////////////////////////////////////////////
CoverAttackPointNodeClass::CoverAttackPointNodeClass (PresetClass *preset)
	:	m_PhysObj (NULL),
		m_CoverSpot (NULL),
		NodeClass (preset)		
{
	_InstanceCount ++;
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	CoverAttackPointNodeClass
//
//////////////////////////////////////////////////////////////////////////////
CoverAttackPointNodeClass::CoverAttackPointNodeClass (const CoverAttackPointNodeClass &src)
	:	m_PhysObj (NULL),
		m_CoverSpot (NULL),
		NodeClass (NULL)
{
	_InstanceCount ++;
	*this = src;
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	~CoverAttackPointNodeClass
//
//////////////////////////////////////////////////////////////////////////////
CoverAttackPointNodeClass::~CoverAttackPointNodeClass (void)
{	
	Remove_From_Scene ();
	MEMBER_RELEASE (m_PhysObj);
	
	//
	//	Free the collision object when all instances have gone away
	//
	_InstanceCount --;
	if (_InstanceCount == 0) {
		MEMBER_RELEASE (_TheCollisionObj);
	}

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
CoverAttackPointNodeClass::Initialize (void)
{
	MEMBER_RELEASE (m_PhysObj);

	//
	//	Create the attack-point render object
	//
	RenderObjClass *render_obj = ::Create_Render_Obj ("WAY_B");
	WWASSERT (render_obj != NULL);
	if (render_obj != NULL) {
		
		// Create the new physics object
		m_PhysObj = new DecorationPhysClass;
		
		//
		// Configure the physics object with information about
		// its new render object and collision data.
		//
		m_PhysObj->Set_Model (render_obj);
		m_PhysObj->Set_Transform (Matrix3D(1));
		m_PhysObj->Set_Collision_Group (EDITOR_COLLISION_GROUP);
		m_PhysObj->Peek_Model ()->Set_User_Data ((PVOID)&m_HitTestInfo, FALSE);
		m_PhysObj->Set_Transform (m_Transform);
		::Set_Model_Collision_Type (m_PhysObj->Peek_Model (), COLLISION_TYPE_0);
		
		// Release our hold on the render object pointer
		MEMBER_RELEASE (render_obj);
	}

	if (_TheCollisionObj == NULL) {

		//
		//	Create the collision physics object
		//	
		PresetClass *preset = PresetMgrClass::Find_Preset ("Walk-Thru");
		if (preset != NULL && preset->Get_Definition ()  != NULL) {
			
			//
			//	Load the assets for this preset and get its definition
			//
			preset->Load_All_Assets ();
			SoldierGameObjDef *definition	= (SoldierGameObjDef *)preset->Get_Definition ();
			
			//
			//	Create the collision physics object
			//
			SoldierGameObj *game_obj = new SoldierGameObj;
			game_obj->Init (*definition);
			MEMBER_ADD (_TheCollisionObj, game_obj->Peek_Physical_Object ());
			game_obj->Set_Delete_Pending ();
		}
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
////////////////////////////////////////////////////////////////
const PersistFactoryClass &
CoverAttackPointNodeClass::Get_Factory (void) const
{	
	return _CoverAttackPointNodePersistFactory;
}


/////////////////////////////////////////////////////////////////
//
//	Save
//
/////////////////////////////////////////////////////////////////
bool
CoverAttackPointNodeClass::Save (ChunkSaveClass &csave)
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
CoverAttackPointNodeClass::Load (ChunkLoadClass &cload)
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
CoverAttackPointNodeClass::Load_Variables (ChunkLoadClass &cload)
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
const CoverAttackPointNodeClass &
CoverAttackPointNodeClass::operator= (const CoverAttackPointNodeClass &src)
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
CoverAttackPointNodeClass::Pre_Export (void)
{
	//
	//	Remove ourselves from the 'system' so we don't get accidentally
	// saved during the export. 
	//
	Add_Ref ();
	if (m_PhysObj != NULL && m_IsInScene) {
		::Get_Scene_Editor ()->Remove_Object (m_PhysObj);
	}

	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Post_Export
//
//////////////////////////////////////////////////////////////////////
void
CoverAttackPointNodeClass::Post_Export (void)
{
	//
	//	Put ourselves back into the system
	//
	if (m_PhysObj != NULL && m_IsInScene) {
		::Get_Scene_Editor ()->Add_Dynamic_Object (m_PhysObj);
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
CoverAttackPointNodeClass::On_Delete (void)
{
	//
	//	Remove ourselves from the cover spot
	//
	if (m_CoverSpot) {
		m_CoverSpot->Remove_Attack_Point (this);
	}

	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Peek_Collision_Obj
//
//////////////////////////////////////////////////////////////////////
PhysClass *
CoverAttackPointNodeClass::Peek_Collision_Obj (void) const
{
	if (_TheCollisionObj != NULL) {
		_TheCollisionObj->Set_Transform (m_Transform);
	}

	return _TheCollisionObj;
}
