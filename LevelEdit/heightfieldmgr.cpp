#include "stdafx.h"

#include "heightfieldmgr.h"
#include "heightfieldeditor.h"
#include "editableheightfield.h"
#include "chunkio.h"
#include "saveload.h"
#include "staticphys.h"
#include "sceneeditor.h"
#include "utils.h"
#include "nodemgr.h"


///////////////////////////////////////////////////////////////////////
// Global singleton instance
///////////////////////////////////////////////////////////////////////
HeightfieldMgrClass	_TheHeightfieldMgrSaveLoadSubsystem;


////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_VARIABLES			= 0x02261012,
	CHUNKID_HEIGHTFIELD
};

enum
{
	VARID_TEXTURE_NAME		= 0x01,
	VARID_CENTER_POINT,
	VARID_SCALE,
	VARID_MAP_TITLE_ID,
	VARID_IS_PLAYER_MARKDER_VISIBLE,
	VARID_ENABLE_VTOL
};


///////////////////////////////////////////////////////////////////////
// Static member initialization
///////////////////////////////////////////////////////////////////////
DynamicVectorClass<EditableHeightfieldClass *>	HeightfieldMgrClass::HeightfieldList;


//////////////////////////////////////////////////////////////////////
//
//	Initialize
//
//////////////////////////////////////////////////////////////////////
void
HeightfieldMgrClass::Initialize (void)
{
	
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Shutdown
//
//////////////////////////////////////////////////////////////////////
void
HeightfieldMgrClass::Shutdown (void)
{
	//
	//	Release each heightfield in our list
	//
	for (int index = 0; index < HeightfieldList.Count (); index ++) {	
		delete HeightfieldList[index];
	}
	
	HeightfieldList.Delete_All ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Save
//
////////////////////////////////////////////////////////////////
bool
HeightfieldMgrClass::Save (ChunkSaveClass &csave)
{
	//
	//	Write the variables
	//
	csave.Begin_Chunk (CHUNKID_VARIABLES);
	csave.End_Chunk ();

	//
	//	Save each heightfield to its own chunk
	//
	for (int index = 0; index < HeightfieldList.Count (); index ++) {
		csave.Begin_Chunk (CHUNKID_HEIGHTFIELD);
			HeightfieldList[index]->Save (csave);
		csave.End_Chunk ();
	}

	return true;
}


////////////////////////////////////////////////////////////////
//
//	Load
//
////////////////////////////////////////////////////////////////
bool
HeightfieldMgrClass::Load (ChunkLoadClass &cload)
{	
	while (cload.Open_Chunk ()) {
		switch (cload.Cur_Chunk_ID ()) {
			
			//
			//	Load all the variables from this chunk
			//
			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;

			case CHUNKID_HEIGHTFIELD:
			{
				//
				//	Load this heightfield from disk
				//
				EditableHeightfieldClass *heightfield = new EditableHeightfieldClass;
				heightfield->Load (cload);

				//
				//	Add this heightfield to our list
				//
				HeightfieldList.Add (heightfield);
				break;
			}

		}

		cload.Close_Chunk ();
	}

	//
	//	Start editing the first heightfield in the list
	//
	if (HeightfieldList.Count () > 0) {
		HeightfieldEditorClass::Load_Materials (HeightfieldList[0]);
	}

	return true;
}


////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
////////////////////////////////////////////////////////////////
void
HeightfieldMgrClass::Load_Variables (ChunkLoadClass &cload)
{
	/*while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {
		}

		cload.Close_Micro_Chunk ();
	}*/

	SaveLoadSystemClass::Register_Post_Load_Callback (this);	
	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	On_Post_Load
//
///////////////////////////////////////////////////////////////////////
void
HeightfieldMgrClass::On_Post_Load (void)
{
	//
	//	Let each heightfield post-load
	//
	for (int index = 0; index < HeightfieldList.Count (); index ++) {
		HeightfieldList[index]->On_Post_Load ();
	}

	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	Assign_Unique_IDs
//
///////////////////////////////////////////////////////////////////////
void
HeightfieldMgrClass::Assign_Unique_IDs (void)
{
	//
	//	Ask each heightfield to update its IDs
	//
	for (int index = 0; index < HeightfieldList.Count (); index ++) {
		HeightfieldList[index]->Assign_Unique_IDs ();
	}

	return ;
}


///////////////////////////////////////////////////////////////////////
//
//	Create_Heightfield
//
///////////////////////////////////////////////////////////////////////
EditableHeightfieldClass *
HeightfieldMgrClass::Create_Heightfield (float width, float height, float density)
{
	//
	//	Create a new heightfield and add it to our list
	//
	EditableHeightfieldClass *heightfield = new EditableHeightfieldClass;	
	heightfield->Set_Dimensions (width, height, density);

	//
	//	Add these objects to our lists
	//
	HeightfieldList.Add (heightfield);
	
	//
	//	Set this heightfield as the current heightfield
	//
	HeightfieldEditorClass::Set_Current_Heightfield (heightfield);

	return heightfield;
}


///////////////////////////////////////////////////////////////////////
//
//	Create_Heightfield
//
///////////////////////////////////////////////////////////////////////
EditableHeightfieldClass *
HeightfieldMgrClass::Create_Heightfield
(
	const char *	heightmap_filename,
	float				width,
	float				height,
	float				density,
	float				scale
)
{
	//
	//	Create a new heightfield and add it to our list
	//
	EditableHeightfieldClass *heightfield = new EditableHeightfieldClass;	
	heightfield->Create (heightmap_filename, width, height, density, scale);

	//
	//	Add these objects to our lists
	//
	HeightfieldList.Add (heightfield);

	//
	//	Set this heightfield as the current heightfield
	//
	HeightfieldEditorClass::Set_Current_Heightfield (heightfield);

	return heightfield;
}

