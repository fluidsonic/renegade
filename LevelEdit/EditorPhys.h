#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __EDITORPHYS_H
#define __EDITORPHYS_H


#include "dynamicphys.h"
#include "pscene.h"
#include "wwdebug.h"
#include "physcoltest.h"
#include "rendobj.h"


///////////////////////////////////////////////////////////////////////////////////
//
//	EditorPhysClass
//
//	Editor's representation of a physics object.
//
///////////////////////////////////////////////////////////////////////////////////
class EditorPhysClass : public DynamicPhysClass
{
	public:

		virtual bool					Cast_Ray(PhysRayCollisionTestClass & raytest);
		virtual bool					Cast_AABox(PhysAABoxCollisionTestClass & boxtest)	{ return false; }
		virtual bool					Cast_OBBox(PhysOBBoxCollisionTestClass & boxtest)	{ return false; }
		virtual void					Timestep (float dt)											{};

		virtual const AABoxClass & Get_Bounding_Box (void) const;
		virtual const Matrix3D &	Get_Transform (void) const;
		virtual void					Set_Transform (const Matrix3D &transform);

		virtual const PersistFactoryClass &	Get_Factory (void) const;
};


///////////////////////////////////////////////////////////////////////////////////
//
//	Cast_Ray
//
inline bool
EditorPhysClass::Cast_Ray (PhysRayCollisionTestClass &raytest)
{
	// State OK?
	WWASSERT (Model != NULL);

	// Cast the ray and return the result
	bool retval = false;	
	if ((Model != NULL) &&
	    (Model->Cast_Ray (raytest))) {
			
		// Success!
		raytest.CollidedPhysObj = this;
		retval = true;
	}

	// Return the true/false result code
	return retval;
}

#endif //__EDITORPHYS_H

