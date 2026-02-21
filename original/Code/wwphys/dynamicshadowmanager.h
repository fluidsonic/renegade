#ifndef DYNAMICSHADOWMANAGER_H
#define DYNAMICSHADOWMANAGER_H

#include "always.h"
#include "vector3.h"


class ChunkLoadClass;
class ChunkSaveClass;
class PhysClass;
class DynTexProjectClass;
class DynamicShadowManagerDefClass;


/**
** DynamicShadowManagerClass
** This is another class designed to be embedded in a physics object.  It's responsibility
** is to update and manage the dynamic shadow projection(s) for its parent.  
*/
class DynamicShadowManagerClass
{
public:

	DynamicShadowManagerClass(PhysClass & parent);
	~DynamicShadowManagerClass(void);
	
	/*
	** Call Update_Shadow in your Post_Timestep_Process function
	*/
	void								Update_Shadow(void);

	/*
	** You can explicitly set the nearz and farz planes for the shadow or 
	** if they are set to -1.0f, the code will handle them automatically.
	** Typically game objects like characters and vehicles should use the
	** automatic setting while cinematics may need a manual setting.  For
	** example, a flying airplane may need to cast a long shadow for the
	** shadow to hit the ground.
	*/
	void								Set_Shadow_Planes(float znear = -1.0f,float zfar = -1.0f);
	void								Get_Shadow_Planes(float * set_znear,float * set_zfar);

	/*
	** Vehicles now have enourmous muzzle flashes that cause the shadow
	** bounding box to be much larger than it should be.  This causes low
	** shadow resolution as well as per-polygon culling visual errors
	*/
	void								Enable_Force_Use_Blob_Box(bool onoff) { ForceUseBlobBox = onoff; }
	bool								Is_Force_Use_Blob_Box_Enabled(void) { return ForceUseBlobBox; }
	void								Set_Blob_Box_Projection_Scale(const Vector3 & scl) { BlobBoxProjectionScale = scl; }
	const Vector3 &				Get_Blob_Box_Projection_Scale(void) { return BlobBoxProjectionScale; }

	/*
	** Accessor to indicate whether we are actually casting a shadow
	*/
	bool								Is_Casting_Shadow(void) { return (Shadow != NULL); }

protected:

	/*
	** Internal shadow support.
	*/
	void								Allocate_Shadow(void);
	void								Release_Shadow(void);

	PhysClass &						Parent;
	DynTexProjectClass *			Shadow;

	float								ShadowNearZ;
	float								ShadowFarZ;

	bool								ForceUseBlobBox;
	Vector3							BlobBoxProjectionScale;
};


inline void DynamicShadowManagerClass::Set_Shadow_Planes(float znear,float zfar)
{
	ShadowNearZ = znear;
	ShadowFarZ = zfar;
}

inline void DynamicShadowManagerClass::Get_Shadow_Planes(float * set_znear,float * set_zfar)
{
	*set_znear = ShadowNearZ;
	*set_zfar = ShadowFarZ;
}


#endif // DYNAMICSHADOWMANAGER_H

