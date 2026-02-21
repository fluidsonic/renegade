
#ifndef PHYSDYNAMICSAVESYSTEM_H
#define PHYSDYNAMICSAVESYSTEM_H

#include "always.h"
#include "saveloadsubsystem.h"

/******************************************************************************************
**
** PhysDynamicSaveSystemClass
** This SaveLoadSubSystem handles saving the dynamic data from the physics system.
**
******************************************************************************************/
class PhysDynamicSaveSystemClass : public SaveLoadSubSystemClass
{
public:
	
	virtual uint32				Chunk_ID (void) const;

protected:

	virtual bool				Contains_Data(void) const;
	virtual bool				Save (ChunkSaveClass &csave);
	virtual bool				Load (ChunkLoadClass &cload);
	virtual const char*		Name() const { return "PhysDynamicSaveSystemClass"; }
	virtual void				On_Post_Load(void);

	/*
	** internal chunk id's
	*/
	enum 
	{
		PDSSC_CHUNKID_SCENE			= 0x00007001,
		PDSSC_CHUNKID_CONSTANTS,
		PDSSC_CHUNKID_PATHMGR
	};
};

/*
** _WWPhysDynamicSaveSystem - global instance of the sub-system for dynamic data
*/
extern PhysDynamicSaveSystemClass _PhysDynamicSaveSystem;

#endif
