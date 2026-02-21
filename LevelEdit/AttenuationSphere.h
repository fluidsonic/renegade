#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __ATTENUATIONSPHERE_H
#define __ATTENUATIONSPHERE_H


#include "Utils.h"
#include "EditorPhys.h"
#include "Vector3.h"


// Forward declarations
class NodeClass;


///////////////////////////////////////////////////////////////////////////////
//
//	AttenuationSphereClass
//
///////////////////////////////////////////////////////////////////////////////
class AttenuationSphereClass : public EditorPhysClass
{
public:

	////////////////////////////////////////////////////////
	// Public contsructors/destructors
	////////////////////////////////////////////////////////
	AttenuationSphereClass (void);
	virtual ~AttenuationSphereClass (void);


	////////////////////////////////////////////////////////
	// Public methods
	////////////////////////////////////////////////////////
	void		Display_Around_Node (const NodeClass &node);
	void		Display_Around_Node (const RenderObjClass &render_obj);
	void		Remove_From_Scene (void);
	void		Set_Color (const Vector3 &color);
	void		Set_Radius (float radius);
	void		Set_Opacity (float opacity);

private:

	////////////////////////////////////////////////////////
	// Private member data
	////////////////////////////////////////////////////////
	bool		m_IsInScene;
	Vector3	m_Color;
	float		m_Radius;
	float		m_Opacity;
};


#endif //__ATTENUATIONSPHERE_H

