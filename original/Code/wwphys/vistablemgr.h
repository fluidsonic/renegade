
#ifndef VISTABLEMGR_H
#define VISTABLEMGR_H

#include "always.h"
#include "simplevec.h"

class VisTableClass;
class CompressedVisTableClass;
class ChunkSaveClass;
class ChunkLoadClass;
class VisDecompressionCacheClass;


/*
** VisTableMgrClass
** This class collects the visibility tables and vis-id allocation into one place. 
** The physics scene contains an instance of this class and will save and load it as
** part of the (L)evel (S)tatic (D)ata file...
*/
class VisTableMgrClass 
{

public:
	
	VisTableMgrClass(void);
	~VisTableMgrClass(void);

	/*
	** Allocation of vis-object and vis-sector IDs
	*/
	void								Reset();
	int								Allocate_Vis_Object_ID(int count = 1);
	int								Allocate_Vis_Sector_ID(int count = 1);
	int								Get_Vis_Table_Size(void) const;
	int								Get_Vis_Table_Count(void) const;
	void								Set_Optimized_Vis_Object_Count(int count);

	/*
	** Access to the actual vis data.  Decompressed tables are cached
	** behind the scenes so just call this whenever you want a vis table.
	** DO NOT EVER modify a vis table you get from this function.  You should
	** always make a copy, modify the copy, then call Update_Vis_Table if
	** you need to modify the contents of a vis table...
	*/
	VisTableClass *				Get_Vis_Table(int id,bool allocate = false);
	void								Update_Vis_Table(int id,VisTableClass * new_pvs);
	bool								Has_Vis_Table(int id);

	/*
	** Pulse this once per frame to purge the LRU cache
	*/
	void								Notify_Frame_Ended(void);

	/*
	** Save/Load interface
	*/
	void								Save(ChunkSaveClass & csave);
	void								Load(ChunkLoadClass & cload);

protected:

	int								VisSectorCount;
	int								VisObjectCount;

	void								Delete_All_Vis_Tables(void);

	SimpleDynVecClass<CompressedVisTableClass *>		VisTables;
	VisDecompressionCacheClass *							Cache;	
	unsigned int												FrameCounter;
};


#endif
