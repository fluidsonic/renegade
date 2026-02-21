#include "StdAfx.h"
#include "AttenuationSphere.h"
#include "SphereObj.h"
#include "Utils.h"
#include "Node.h"
#include "SceneEditor.h"


///////////////////////////////////////////////////////////////////////////////
//
//	AttenuationSphereClass
//
///////////////////////////////////////////////////////////////////////////////
AttenuationSphereClass::AttenuationSphereClass (void)
	:	m_IsInScene (false),
		m_Color (1, 1, 1),
		m_Radius (1.0F),
		m_Opacity (1.0F)
{
	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	~AttenuationSphereClass
//
///////////////////////////////////////////////////////////////////////////////
AttenuationSphereClass::~AttenuationSphereClass (void)
{
	Remove_From_Scene ();
	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Display_Around_Node
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Display_Around_Node (const NodeClass &node)
{
	RenderObjClass *render_obj = node.Peek_Render_Obj ();
	if (render_obj != NULL) {
		Display_Around_Node (*render_obj);
	}

	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Display_Around_Node
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Display_Around_Node (const RenderObjClass &render_obj)
{
	//
	//	Make sure we have a sphere object
	//
	SphereRenderObjClass *sphere = (SphereRenderObjClass *)Peek_Model ();
	if (sphere == NULL) {
		sphere = new SphereRenderObjClass;
		sphere->Set_Flag (SphereRenderObjClass::USE_ALPHA_VECTOR, false);
		Set_Model (sphere);

		Set_Color (m_Color);
		Set_Radius (m_Radius);
		Set_Opacity (m_Opacity);

		MEMBER_RELEASE (sphere);
	}

	//
	//	Update the sphere's position
	//
	Set_Transform (render_obj.Get_Transform ());

	//
	//	Put the sphere into the scene
	//
	if (m_IsInScene == false) {
		::Get_Scene_Editor ()->Add_Dynamic_Object (this);
		m_IsInScene = true;
	}

	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Remove_From_Scene
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Remove_From_Scene (void)
{
	//
	//	Remove the sphere from the scene
	//
	if (m_IsInScene) {
		::Get_Scene_Editor ()->Remove_Object (this);
		m_IsInScene = false;
	}

	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Set_Color
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Set_Color (const Vector3 &color)
{
	SphereRenderObjClass *sphere = (SphereRenderObjClass *)Peek_Model ();
	SANITY_CHECK (sphere != NULL) {
		return ;
	}

	m_Color = color;
	sphere->Set_Color (m_Color);
	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Set_Radius
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Set_Radius (float radius)
{
	SphereRenderObjClass *sphere = (SphereRenderObjClass *)Peek_Model ();
	SANITY_CHECK (sphere != NULL) {
		return ;
	}

	m_Radius = radius;
	sphere->Set_Extent (Vector3 (m_Radius, m_Radius, m_Radius));	
	sphere->Set_Transform (sphere->Get_Transform ());
	Update_Cull_Box ();
	return ;
}


///////////////////////////////////////////////////////////////////////////////
//
//	Set_Opacity
//
///////////////////////////////////////////////////////////////////////////////
void
AttenuationSphereClass::Set_Opacity (float opacity)
{
	SphereRenderObjClass *sphere = (SphereRenderObjClass *)Peek_Model ();
	SANITY_CHECK (sphere != NULL) {
		return ;
	}

	m_Opacity = opacity;
	sphere->Set_Alpha (m_Opacity);
	return ;
}


