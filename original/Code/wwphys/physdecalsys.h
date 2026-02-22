#pragma once

#include "global.h"

#include "vector.h"
#include "simplevec.h"
#include "multilist.h"
#include "decalsys.h"

class MeshClass;
class PhysicsSceneClass;
class CameraClass;
class PhysClass;
class DistAlphaVPClass;

/**
** PhysDecalSysClass
** This derived DecalSystemClass provides two pools of decals.  One pool is a 
** fixed size buffer of decals which are recycled when the buffer fills up.  The second
** pool is for "permanent" decals.
**
** Saving and loading of decals is currently not possible so the external user will have
** to track the parameters used to create any decals that he wants to survive across
** a save-load.  
**
** The decal ID used by this system is formatted in a way so that we 
*/
class PhysDecalSysClass : public DecalSystemClass
{
public:

	PhysDecalSysClass(PhysicsSceneClass * parent_scene);
	virtual ~PhysDecalSysClass(void);

	/*
	** Create_Decal - this is the interface typically used by PhysicsSceneClass to create new decals
	** Update_Decal_Fade_Distances - this updates the alpha fading parameters
	*/
	void										Update_Decal_Fade_Distances(const CameraClass & camera);
	int										Create_Decal(		const Matrix3D &	tm,
																		const char *		texture_name,
																		float					radius,
																		bool					is_permanent,
																		bool					apply_to_translucent_meshes,
																		PhysClass *			only_this_obj);
	
	bool										Remove_Decal(uint32_t id);

	/*
	**	Create and release DecalGenerators.  Note that this is the point at which the 
	** decal system can track "logical" decals.  The generator will keep an internal list
	** of all of the render objects which generated decals which you should copy if you
	** want to track them (e.g. if you want to cap the maximum number of active decals and
	** kill the old ones...)
	*/
	virtual void							Unlock_Decal_Generator(DecalGeneratorClass * generator);

	/*
	** When a decal-mesh is destroyed, it must inform the DecalSystem.  Otherwise, systems 
	** which track decals can get dangling pointers.
	*/
	virtual void							Decal_Mesh_Destroyed(uint32_t decal_id,DecalMeshClass * mesh);

	/*
	** Control over the size of the temporary decal array.  When this array is filled, the
	** oldest decals are removed as new decals are added.
	*/
	void										Set_Temporary_Decal_Pool_Size(int count);
	int										Get_Temporary_Decal_Pool_Size(void);

protected:

	virtual uint32_t							Generate_Decal_Id(void);
	bool										is_decal_id_permanent(uint32_t id);
	bool										internal_remove_decal(uint32_t id,MeshClass * mesh);

	void										allocate_resources(void);
	void										release_resources(void);

	/**
	** LogicalDecalClass
	** This class is used to track all of the meshes that were affected when a 
	** decal is generated.
	*/
	class LogicalDecalClass : public MultiListObjectClass
	{
	public:
		LogicalDecalClass(void);
		~LogicalDecalClass(void);

		bool										operator == (const LogicalDecalClass & that) { return false; }
		bool										operator != (const LogicalDecalClass & that) { return true; }

		void										Reset(void);
		void										Init(DecalGeneratorClass * gen);

		uint32_t									DecalID;
		SimpleDynVecClass<MeshClass *>	Meshes;
	};

	PhysicsSceneClass *						ParentScene;				// scene that this decal system works with
	bool											CreatePermanentDecals;	// internal setting, are we creating permanent or temporary decals

	uint32_t										NextTempDecalIndex;		// index of the next temporary decal
	VectorClass<LogicalDecalClass>		TempDecals;					// array of logical decals for 
	MultiListClass<LogicalDecalClass>	PermanentDecals;			// linked list of permanent decals

	VertexMaterialClass *					DecalMaterial;				// material used by all decals in WWPhys
	ShaderClass									DecalShader;				// shader used by all decals in WWPhys

	DistAlphaVPClass*							DecalDistAlphaVP;
};
