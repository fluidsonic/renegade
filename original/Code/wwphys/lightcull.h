#if defined(_MSC_VER)
#pragma once
#endif

#ifndef LIGHTCULL_H
#define LIGHTCULL_H

#include "aabtreecull.h"
#include "lightphys.h"
#include "ww3d.h"


/*
** LightCullClass
** This is a derived axis-aligned bounding box tree for spatially subdividing the
** static (non-moving) lights in a level.   
*/
class StaticLightCullClass : public TypedAABTreeCullSystemClass<LightPhysClass>
{

public:

	StaticLightCullClass(void);
	~StaticLightCullClass(void);

	/*
	** StaticLightCullClass over-rides the add and remove methods in order to add
	** vis-data support.
	*/
	virtual void		Add_Object(LightPhysClass * obj);
	virtual void		Remove_Object(LightPhysClass * obj);
	virtual void		Update_Culling(CullableClass * obj);

	/*
	** Visibility.  Each static light allocates a vis-sector which is used
	** for occlusion culling when deciding which dynamic objects should be 
	** affected by the light.
	*/
	void					Assign_Vis_IDs(void);
	void					Merge_Vis_Sector_IDs(uint32 id0,uint32 id1);

	/*
	** Save-Load support.  
	** The physics scene will call this class's save function from 
	** PhysicsSceneClass::Save_Level_Static_Data. 
	*/
	void					Save_Static_Data(ChunkSaveClass & csave);
	void					Load_Static_Data(ChunkLoadClass & cload);

};


#endif //LIGHTCULL_H

