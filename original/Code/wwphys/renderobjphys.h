#if defined(_MSC_VER)
#pragma once
#endif

#ifndef RENDEROBJPHYS_H
#define RENDEROBJPHYS_H

#include "always.h"
#include "dynamicphys.h"
#include "pscene.h"
#include "wwdebug.h"
#include "physcoltest.h"


/**
** RenderObjPhysClass
** The *ONLY* purpose of this class is as a wrapper when render objects are added 
** directly to the physics scene.  This class plugs a pointer back to it into the UserData 
** field of its render object.  If you find yourself creating these objects,
** consider using DecorationPhysClass instead...
*/
class RenderObjPhysClass : public DynamicPhysClass
{
public:

	RenderObjPhysClass(void);
	~RenderObjPhysClass(void);

	virtual void					Timestep(float dt)											{ } 
	virtual void					Set_Model(RenderObjClass * model);
	virtual bool					Cast_Ray(PhysRayCollisionTestClass & raytest);
	virtual bool					Cast_AABox(PhysAABoxCollisionTestClass & boxtest);
	virtual bool					Cast_OBBox(PhysOBBoxCollisionTestClass & boxtest);

	virtual const AABoxClass & Get_Bounding_Box(void) const;
	virtual const Matrix3D &	Get_Transform(void) const;
	virtual void					Set_Transform(const Matrix3D & m);

	virtual RenderObjPhysClass *	As_RenderObjPhysClass(void)							{ return this; }

	/*
	** Save-Load 
	*/
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual void								On_Post_Load (void);


private:

	// Not implemented...
	RenderObjPhysClass(const RenderObjPhysClass &);
	RenderObjPhysClass & operator = (const RenderObjPhysClass &);

};


#endif

