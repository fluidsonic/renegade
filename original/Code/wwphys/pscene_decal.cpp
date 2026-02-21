#include "pscene.h"
#include "colmathaabox.h"
#include "assetmgr.h"
#include "vertmaterial.h"
#include "camera.h"
#include "physdecalsys.h"
#include "phys.h"

/*
** PhysicsSceneClass Decal Methods 
*/

void PhysicsSceneClass::Allocate_Decal_Resources(void)
{
	DecalSystem = new PhysDecalSysClass(this);
}	

void PhysicsSceneClass::Release_Decal_Resources(void)
{
	if (DecalSystem != NULL) {
		delete DecalSystem;
	}
}

int PhysicsSceneClass::Create_Decal
(	
	const Matrix3D &	tm,
	const char *		texture_name,
	float					radius,
	bool					is_permanent,
	bool					apply_to_translucent_meshes,
	PhysClass *			only_this_obj
)
{
	return DecalSystem->Create_Decal(tm,texture_name,radius,is_permanent,apply_to_translucent_meshes,only_this_obj);
}

bool PhysicsSceneClass::Remove_Decal(uint32 id)
{
	return DecalSystem->Remove_Decal(id);
}

