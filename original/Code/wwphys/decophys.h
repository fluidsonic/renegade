#pragma once

#include "global.h"

#include "dynamicphys.h"
#include "pscene.h"
#include "physcoltest.h"

class DecorationPhysDefClass;

/**
** DecorationPhysClass
** Decoration object.  It is an object that can be added into the dynamic physics
** system but doesn't really do anything.  It can be collided against if collision is
** enabled in its render object...
*/
class DecorationPhysClass : public DynamicPhysClass
{
public:

	DecorationPhysClass(void);
	virtual DecorationPhysClass *	As_DecorationPhysClass(void)							{ return this; }
	
	void									Init(const DecorationPhysDefClass & def);
	virtual void						Timestep(float dt)										{ } 
	virtual void						Set_Model(RenderObjClass * model);
	
	virtual bool						Cast_Ray(PhysRayCollisionTestClass & raytest);
	virtual bool						Cast_AABox(PhysAABoxCollisionTestClass & boxtest);
	virtual bool						Cast_OBBox(PhysOBBoxCollisionTestClass & boxtest);

	virtual bool						Intersection_Test(PhysAABoxIntersectionTestClass & test);
	virtual bool						Intersection_Test(PhysOBBoxIntersectionTestClass & test);

	virtual const AABoxClass &		Get_Bounding_Box(void) const;
	virtual const Matrix3D &		Get_Transform(void) const;
	virtual void						Set_Transform(const Matrix3D & m);
	virtual void						Get_Shadow_Blob_Box(AABoxClass * set_obj_space_box);

	// save-load system
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);		
	virtual void								On_Post_Load(void);

private:

	// Not implemented...
	DecorationPhysClass(const DecorationPhysClass &);
	DecorationPhysClass & operator = (const DecorationPhysClass &);

	AABoxClass							ObjSpaceWorldBox;
};

/**
** DecorationPhysDefClass
** Definition data structure for DecorationPhysClass
*/
class DecorationPhysDefClass : public DynamicPhysDefClass
{
public:
	
	DecorationPhysDefClass(void);
	
	// From DefinitionClass
	virtual uint32_t								Get_Class_ID (void) const;
	virtual PersistClass *					Create(void) const;

	// From PhysDefClass
	virtual const char *						Get_Type_Name(void);
	virtual bool								Is_Type(const char *);

	// From PersistClass
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save(ChunkSaveClass &csave);
	virtual bool								Load(ChunkLoadClass &cload);

	//	Editable interface requirements

protected:
	
	friend class StaticPhysClass;
};
