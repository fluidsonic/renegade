#pragma once

#include "global.h"

#include "decophys.h"
#include "rendobj.h"

/**
** LightPhysClass
** Just a class that is used by the light culling system.  Its main purposes
** are to enforce that the user (editor) places only lights into the system,
** to add save and load functionality, and to add any special effects or processing
** like lens flares and shadows.
*/
class LightPhysClass : public DecorationPhysClass
{
public:
	
	LightPhysClass(bool auto_allocate_light = false);
	virtual LightPhysClass *				As_LightPhysClass(void)		{ return this; }

	void											Set_Model(RenderObjClass * model);

	/*
	** Visibility ID's.  Static lights can store a vis SECTOR id so that they
	** have a pre-calculated pvs for occlusion culling.
	*/
	void											Set_Vis_Sector_ID(int new_id)		{ VisSectorID = new_id; }	
	int											Get_Vis_Sector_ID(void) const		{ return VisSectorID; }

	/*
	** Group ID's.  Static lights can be assigned a "Group ID".  This id can be used by
	** external code.  For example, the building code uses the Group ID to differentiate between
	** lights that should be 'on' when the power to the building is 'on' and lights that should
	** be 'on' when the power is 'off'.
	*/
	void											Set_Group_ID(int new_id)			{ GroupID = new_id; }
	int											Get_Group_ID(void) const			{ return GroupID; }

	/*
	** Visibility checking
	*/
	int											Is_Vis_Object_Visible(int vis_object_id);

	/*
	** Save-Load system
	*/
	virtual const PersistFactoryClass &	Get_Factory (void) const;
	virtual bool								Save (ChunkSaveClass &csave);
	virtual bool								Load (ChunkLoadClass &cload);
	virtual void								On_Post_Load(void);

private:

	int											VisSectorID;			// static lights have a pvs.
	int											GroupID;					// group id, used by external code

	// Not implemented...
	LightPhysClass(const LightPhysClass &);
	LightPhysClass & operator = (const LightPhysClass &);
};
