#ifndef DYANMICANIMPHYS_H
#define DYNAMICANIMPHYS_H

#include "always.h"
#include "decophys.h"
#include "animcollisionmanager.h"
#include "dynamicshadowmanager.h"

class ChunkLoadClass;
class ChunkSaveClass;
class DynamicAnimPhysDefClass;

/**
** DynamicAnimPhysClass
** This class manages dynamic animations.  These are similar to static anims but they can
** be dynamically created and destroyed and their bounding boxes can be animated (since they
** are placed into the dynamic culling system.  However, since they are dynamic objects,
** they do not VIS or cull as accurately.  An example of a dynamic animation would be an
** animated plane that flys across the level while an example of a static animation would
** be an animating elevator.
*/
class DynamicAnimPhysClass : public DecorationPhysClass
{
public:

	DynamicAnimPhysClass(void);
	virtual ~DynamicAnimPhysClass(void);

	virtual DynamicAnimPhysClass *		As_DynamicAnimPhysClass(void)								{ return this; }
	const DynamicAnimPhysDefClass *		Get_DynamicAnimPhysDef(void);
	
	void											Init(const DynamicAnimPhysDefClass & def);
	virtual void								Set_Model(RenderObjClass * model);

	virtual bool								Needs_Timestep(void)											{ return true; }
	virtual void								Timestep(float dt);
	virtual void								Post_Timestep_Process(void);

	/*
	** State Import/Export and Save/Load
	*/
	virtual bool								Has_Dynamic_State(void)										{ return true; }

	/*
	** save-load system
	*/
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);		
	virtual void								On_Post_Load(void);

	/*
	** Animation and animated collision control
	*/
	AnimCollisionManagerClass &			Get_Animation_Manager(void)								{ return AnimManager; }

protected:
	
	void											Update_Cached_Model_Parameters(void);
	void											Reset_Mappers(RenderObjClass * model);

	AnimCollisionManagerClass				AnimManager;
	DynamicShadowManagerClass				ShadowManager;
	
private:

	// Not implemented...
	DynamicAnimPhysClass(const DynamicAnimPhysClass &);
	DynamicAnimPhysClass & operator = (const DynamicAnimPhysClass &);

};

/**
** DynamicAnimPhysDefClass
** Definition data structure for DynamicAnimPhysClass
*/
class DynamicAnimPhysDefClass : public DecorationPhysDefClass
{
public:
	
	DynamicAnimPhysDefClass(void);
	
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
	DECLARE_EDITABLE(DynamicAnimPhysDefClass,DecorationPhysDefClass);

protected:
	
	// Animation and animated collision support
	AnimCollisionManagerDefClass			AnimManagerDef;

	bool											CastsShadows;
	float											ShadowNearZ;
	float											ShadowFarZ;

	friend class DynamicAnimPhysClass;
};

/*
** Inlines
*/
inline const DynamicAnimPhysDefClass * DynamicAnimPhysClass::Get_DynamicAnimPhysDef(void)
{
	return (DynamicAnimPhysDefClass *)Definition;
}

#endif //DYNAMICANIMPHYS_H

