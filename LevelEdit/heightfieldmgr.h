#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __HEIGHTFIELDMGR_H
#define __HEIGHTFIELDMGR_H

#include "vector.h"
#include "saveloadsubsystem.h"
#include "editorchunkids.h"


///////////////////////////////////////////////////////////////////////
// Forward declarations
///////////////////////////////////////////////////////////////////////
class EditableHeightfieldClass;
class StaticPhysClass;

///////////////////////////////////////////////////////////////////////
// Global singleton instance
///////////////////////////////////////////////////////////////////////
extern class HeightfieldMgrClass _TheHeightfieldMgrSaveLoadSubsystem;


//////////////////////////////////////////////////////////////////////
//
//	HeightfieldMgrClass
//
//////////////////////////////////////////////////////////////////////
class HeightfieldMgrClass : public SaveLoadSubSystemClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Initialization support
	//
	static void				Initialize (void);
	static void				Shutdown (void);

	//
	//	Creation
	//
	static EditableHeightfieldClass *	Create_Heightfield (const char *heightmap_filename, float width, float height, float density, float scale);
	static EditableHeightfieldClass *	Create_Heightfield (float width, float height, float density);

	//
	//	Information
	//
	static int				Get_Heightfield_Count (void)	{ return HeightfieldList.Count (); }

	//
	//	ID Support
	//
	static void				Assign_Unique_IDs (void);
	
	//
	//	From SaveLoadSubSystemClass
	//
	uint32					Chunk_ID (void) const			{ return CHUNKID_HEIGHTFIELD_MGR; }

protected:

	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////

	//
	//	From SaveLoadSubSystemClass
	//
	bool						Save (ChunkSaveClass &csave);
	bool						Load (ChunkLoadClass &cload);
	void						On_Post_Load (void);
	const char *			Name (void) const					{ return "HeightfieldMgrClass"; }	

	//
	//	Save load support
	//
	void						Load_Variables (ChunkLoadClass &cload);
	
private:
	
	///////////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////////
	static DynamicVectorClass<EditableHeightfieldClass *>	HeightfieldList;
};


#endif //__HEIGHTFIELDMGR_H
