#pragma once

#include "global.h"

#include "coltest.h"

class PhysClass;

///////////////////////////////////////////////////////////////////////////
// 
// Derived versions of the Collision Test Classes which contain
// a pointer to the 'CollidedPhysObj'.  This will be set to point at the 
// phys object collided with (if any).
//
///////////////////////////////////////////////////////////////////////////
class PhysRayCollisionTestClass : public RayCollisionTestClass
{
public:

	/*
	** To create a PhysRayCollisionTest, you need the following
	** ray - the ray you want to test
	** res - pointer to a result struct to put results in
	** group - collision group of the object making query
	*/
	PhysRayCollisionTestClass(const LineSegClass & ray,CastResultStruct * res,int group,int type = COLLISION_TYPE_PROJECTILE) :
		RayCollisionTestClass(ray,res,type), 
		CollidedPhysObj(NULL),
		CollisionGroup(group),
		CheckStaticObjs(true),
		CheckDynamicObjs(true)
	{
	}

	PhysClass *		CollidedPhysObj;
	int				CollisionGroup;
	bool				CheckStaticObjs;
	bool				CheckDynamicObjs;

private:

	// not implemented:
	PhysRayCollisionTestClass(const PhysRayCollisionTestClass & );
	PhysRayCollisionTestClass & operator = (const PhysRayCollisionTestClass & );

};

class PhysAABoxCollisionTestClass : public AABoxCollisionTestClass
{
public:

	/*
	** To create a PhysAABoxCollisionTest, you need the following
	** box - the axis aligned box you are testing
	** move - its move vector
	** res - pointer to a result struct to put results in
	** group - collision group of the object making query
	*/
	PhysAABoxCollisionTestClass(const AABoxClass & aabox,const Vector3 & move,CastResultStruct * res,int group,int type = COLLISION_TYPE_PHYSICAL) : 
		AABoxCollisionTestClass(aabox,move,res,type), 
		CollidedPhysObj(NULL),
		CollisionGroup(group),
		CheckStaticObjs(true),
		CheckDynamicObjs(true)
	{
	}

	PhysClass *		CollidedPhysObj;
	int				CollisionGroup;
	bool				CheckStaticObjs;
	bool				CheckDynamicObjs;

private:

	// not implemented:
	PhysAABoxCollisionTestClass(const PhysAABoxCollisionTestClass & );
	PhysAABoxCollisionTestClass & operator = (const PhysAABoxCollisionTestClass & );

};

class PhysOBBoxCollisionTestClass : public OBBoxCollisionTestClass
{
public:

	/*
	** To create a PhysOBBoxCollisionTest, you need the following
	** obbox - the oriented box you are testing
	** move - its move vector
	** res - pointer to a result struct to put results in
	** group - collision group of the object making query
	*/
	PhysOBBoxCollisionTestClass(const OBBoxClass & box,const Vector3 & move,CastResultStruct * res,int group,int type = COLLISION_TYPE_PHYSICAL) : 
		OBBoxCollisionTestClass(box,move,res,type), 
		CollidedPhysObj(NULL),
		CollisionGroup(group),
		CheckStaticObjs(true),
		CheckDynamicObjs(true)
	{
	}

	PhysClass *		CollidedPhysObj;
	int				CollisionGroup;
	bool				CheckStaticObjs;
	bool				CheckDynamicObjs;

private:

	// not implemented:
	PhysOBBoxCollisionTestClass(const PhysOBBoxCollisionTestClass & );
	PhysOBBoxCollisionTestClass & operator = (const PhysOBBoxCollisionTestClass & );

};
