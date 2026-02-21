
#ifndef PHYSAABTREECULL_H
#define PHYSAABTREECULL_H

#include "always.h"
#include "aabtreecull.h"
#include "phys.h"

class PhysicsSceneClass;
class StringClass;

/*
** PhysAABTreeCullClass
** This is a derived axis-aligned bounding box tree for use with the physics library.
** It adds collision detection queries to the base AABTree
*/
class PhysAABTreeCullClass : public TypedAABTreeCullSystemClass<PhysClass>
{

public:

	PhysAABTreeCullClass(PhysicsSceneClass * pscene);
	~PhysAABTreeCullClass(void);

	/*
	** Verify that the tree is valid, (all boxes contain their children, all objects contained in their node)
	*/
	bool					Verify(StringClass & set_error_report);

	/*
	** Collision detection
	*/
	bool					Cast_Ray(PhysRayCollisionTestClass & raytest);
	bool					Cast_AABox(PhysAABoxCollisionTestClass & boxtest);
	bool					Cast_OBBox(PhysOBBoxCollisionTestClass & boxtest);
	
	bool					Intersection_Test(PhysAABoxIntersectionTestClass & boxtest);
	bool					Intersection_Test(PhysOBBoxIntersectionTestClass & boxtest);
	bool					Intersection_Test(PhysMeshIntersectionTestClass & meshtest);

	/*
	** Debugging options, disable hierarchical vis culling
	*/
	static void			Enable_Hierarchical_Vis_Culling(bool onoff)	{ _HierarchicalVisCullingEnabled = onoff; }
	static bool			Is_Hierarchical_Vis_Culling_Enabled(void)		{ return _HierarchicalVisCullingEnabled; }

protected:

	/*
	** Internal functions
	*/
	bool					Verify_Recursive(AABTreeNodeClass * node,StringClass & error_report);
	bool					Cast_Ray_Recursive(AABTreeNodeClass * node,PhysRayCollisionTestClass & raytest);
	bool					Cast_AABox_Recursive(AABTreeNodeClass * node,PhysAABoxCollisionTestClass & boxtest);
	bool					Cast_OBBox_Recursive(AABTreeNodeClass * node,PhysOBBoxCollisionTestClass & boxtest);
		
	/*
	** Members
	*/
	PhysicsSceneClass *	Scene;				// scene that we are a member of

	static bool				_HierarchicalVisCullingEnabled;
};

inline bool PhysAABTreeCullClass::Cast_Ray(PhysRayCollisionTestClass & raytest)
{
	return Cast_Ray_Recursive(RootNode,raytest);
}

inline bool PhysAABTreeCullClass::Cast_AABox(PhysAABoxCollisionTestClass & boxtest)
{
	return Cast_AABox_Recursive(RootNode,boxtest);
}

inline bool PhysAABTreeCullClass::Cast_OBBox(PhysOBBoxCollisionTestClass & boxtest)
{
	return Cast_OBBox_Recursive(RootNode,boxtest);
}

#endif // PHYSAABTREECULL_H
