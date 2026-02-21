#include "stdafx.h"
#include "transitionnode.h"
#include "sceneeditor.h"

#include "filemgr.h"
#include "_assetmgr.h"
#include "editorassetmgr.h"
#include "w3d_file.h"
#include "cameramgr.h"
#include "collisiongroups.h"
#include "persistfactory.h"
#include "editorchunkids.h"
#include "preset.h"
#include "spawn.h"
#include "physicalgameobj.h"
#include "presetmgr.h"
#include "decophys.h"
#include "nodemgr.h"
#include "modelutils.h"


//////////////////////////////////////////////////////////////////////////////
//	Persist factory
//////////////////////////////////////////////////////////////////////////////
SimplePersistFactoryClass<TransitionNodeClass, CHUNKID_NODE_TRANSITION> _TransitionNodePersistFactory;


//////////////////////////////////////////////////////////////////////////////
//
//	TransitionNodeClass
//
//////////////////////////////////////////////////////////////////////////////
TransitionNodeClass::TransitionNodeClass (PresetClass *preset)
	:	m_PhysObj (NULL),
		m_TransitionObj (NULL),
		NodeClass (preset)
{
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	TransitionNodeClass
//
//////////////////////////////////////////////////////////////////////////////
TransitionNodeClass::TransitionNodeClass (const TransitionNodeClass &src)
	:	m_PhysObj (NULL),
		m_TransitionObj (NULL),
		NodeClass (NULL)
{
	*this = src;
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	~TransitionNodeClass
//
//////////////////////////////////////////////////////////////////////////////
TransitionNodeClass::~TransitionNodeClass (void)
{	
	Remove_From_Scene ();
	MEMBER_RELEASE (m_PhysObj);

	if (m_TransitionObj != NULL) {
		m_TransitionObj->Set_Delete_Pending ();
		m_TransitionObj = NULL;
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
TransitionNodeClass::Initialize (void)
{
	if (m_TransitionObj != NULL) {
		m_TransitionObj->Set_Delete_Pending ();
		m_TransitionObj = NULL;
	}

	MEMBER_RELEASE (m_PhysObj);

	TransitionGameObjDef *definition = static_cast<TransitionGameObjDef *> (m_Preset->Get_Definition ());
	if (definition != NULL) {

		//	Create the transition object
		m_TransitionObj = (TransitionGameObj *)definition->Create ();

		// Create the new physics object
		m_PhysObj = new DecorationPhysClass;
		
		//
		// Configure the physics object with information about
		// its new render object and collision data.
		//
		m_PhysObj->Set_Model_By_Name ("TRANSBOX");
		m_PhysObj->Set_Transform (Matrix3D(1));
		m_PhysObj->Set_Collision_Group (EDITOR_COLLISION_GROUP);
		m_PhysObj->Peek_Model ()->Set_User_Data ((PVOID)&m_HitTestInfo, FALSE);
		::Set_Model_Collision_Type (m_PhysObj->Peek_Model (), COLLISION_TYPE_6);
		Set_Transform (m_Transform);

		//
		//	Remove the transitions from the level if the transition
		// object is not in the scene
		//
		if (m_IsInScene == false) {
			m_TransitionObj->Destroy_Transitions ();
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
TransitionNodeClass::Get_Factory (void) const
{	
	return _TransitionNodePersistFactory;
}


/////////////////////////////////////////////////////////////////
//
//	operator=
//
/////////////////////////////////////////////////////////////////
const TransitionNodeClass &
TransitionNodeClass::operator= (const TransitionNodeClass &src)
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
TransitionNodeClass::Pre_Export (void)
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
TransitionNodeClass::Post_Export (void)
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
//	Add_To_Scene
//
//////////////////////////////////////////////////////////////////////
void
TransitionNodeClass::Add_To_Scene (void)
{
	if (m_TransitionObj != NULL) {
		m_TransitionObj->Create_Transitions ();
	}

	NodeClass::Add_To_Scene ();
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Remove_From_Scene
//
//////////////////////////////////////////////////////////////////////
void
TransitionNodeClass::Remove_From_Scene (void)
{
	if (m_TransitionObj != NULL) {
		m_TransitionObj->Destroy_Transitions ();
	}

	NodeClass::Remove_From_Scene ();
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Find_Transition
//
//////////////////////////////////////////////////////////////////////
int
TransitionNodeClass::Find_Transition (TransitionDataClass::StyleType type)
{
	int index = -1;

	int count = Get_Transition_Count ();
	for (	int trans_index = 0;
			(index == -1) && (trans_index < count);
			trans_index ++)
	{
		//
		//	Is this the type transition we are looking for?
		//
		TransitionInstanceClass *transition = Get_Transition (trans_index);
		if (transition != NULL && transition->Get_Type () == type) {
			index = trans_index;
		}
	}

	return index;
}
