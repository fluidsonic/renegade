#include "StdAfx.H"
#include "EditorPhys.H"
#include "persist.h"
#include "persistfactory.h"
#include "editorchunkids.h"

//
// Persist factory for EditorPhysClass
//
SimplePersistFactoryClass<EditorPhysClass, CHUNKID_EDITOR_PHYS>	_EditorPhysFactory;


///////////////////////////////////////////////////////////////////////////////////
//
//	Get_Bounding_Box
//
const AABoxClass &
EditorPhysClass::Get_Bounding_Box (void) const
{
	assert(Model);
	return Model->Get_Bounding_Box ();
}


///////////////////////////////////////////////////////////////////////////////////
//
//	Get_Transform
//
const Matrix3D &
EditorPhysClass::Get_Transform (void) const
{
	assert(Model);
	return Model->Get_Transform();
}


///////////////////////////////////////////////////////////////////////////////////
//
//	Get_Transform
//
void
EditorPhysClass::Set_Transform (const Matrix3D &transform)
{
	// Note: this kind of object never causes collisions so we
	// can just warp it to the users desired position.  However,
	// we do need to tell the base class that we moved so that
	// it can tell the scene to update us in the culling system
	assert(Model);
	Model->Set_Transform (transform);
	Update_Cull_Box();
	return ;
}


///////////////////////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
///////////////////////////////////////////////////////////////////////////////////
const PersistFactoryClass &
EditorPhysClass::Get_Factory (void) const
{
	//
	//	This should NEVER be called...
	//
	ASSERT (0);
	return _EditorPhysFactory;
}
