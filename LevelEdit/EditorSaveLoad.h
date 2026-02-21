#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __EDITOR_SAVE_LOAD_H
#define __EDITOR_SAVE_LOAD_H

#include "saveloadsubsystem.h"
#include "vector.h"
#include "bittype.h"
#include "editorchunkids.h"


//////////////////////////////////////////////////////////////////////////
// Singleton instance
//////////////////////////////////////////////////////////////////////////
extern class EditorSaveLoadClass _TheEditorSaveLoadSubsystem;


//////////////////////////////////////////////////////////////////////////
//
//	EditorSaveLoadClass
//
//////////////////////////////////////////////////////////////////////////
class EditorSaveLoadClass : public SaveLoadSubSystemClass
{
public:
	
	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	EditorSaveLoadClass (void)				{ }
	virtual ~EditorSaveLoadClass (void)	{ }

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	
	// From SaveLoadSubSystemClass
	virtual uint32				Chunk_ID (void) const;
	virtual void				On_Post_Load (void);
	virtual const char *		Name (void) const			{ return "EditorSaveLoadClass"; }

	static void					Load_Level (LPCTSTR filename);
	static void					Save_Level (LPCTSTR filename);

	static void					Export_Dynamic_Objects (LPCTSTR filename);
	static void					Import_Dynamic_Objects (LPCTSTR filename);

	// Vis methods
	static void					Set_Loaded_Vis_Valid (bool onoff)	{ m_LoadedValidVis = onoff; }
	static bool					Is_Loaded_Vis_Valid (void)				{ return m_LoadedValidVis; }
	
protected:

	//////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////
	virtual bool				Contains_Data(void) const;
	virtual bool				Save (ChunkSaveClass &csave);
	virtual bool				Load (ChunkLoadClass &cload);


	//bool							Save_Presets (ChunkSaveClass &csave);
	bool							Load_Micro_Chunks (ChunkLoadClass &cload);

	static void					Save_Level_Data(ChunkSaveClass & chunk_save);
	static void					Save_Light_Solve(ChunkSaveClass & chunk_save);

private:

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	static bool					m_LoadedValidVis;

};



//////////////////////////////////////////////////////////////////////////
//
//	PathfindImportExportSaveLoadClass
//
//////////////////////////////////////////////////////////////////////////
class PathfindImportExportSaveLoadClass : public SaveLoadSubSystemClass
{
public:
	
	//////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	//////////////////////////////////////////////////////////////
	PathfindImportExportSaveLoadClass (void)				{ }
	virtual ~PathfindImportExportSaveLoadClass (void)	{ }

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	
	//
	// Inherited
	//
	virtual uint32				Chunk_ID (void) const	{ return CHUNKID_EDITOR_PATHFIND_IMPORTER_EXPORTER; }
	virtual void				On_Post_Load (void)		{}
	virtual const char *		Name (void) const			{ return "PathfindImportExportSaveLoadClass"; }

	static void					Export_Pathfind (LPCTSTR filename);
	static void					Import_Pathfind (LPCTSTR filename);
	
protected:

	//////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////
	virtual bool				Contains_Data(void) const		{ return true; }
	virtual bool				Save (ChunkSaveClass &csave);
	virtual bool				Load (ChunkLoadClass &cload);

	bool							Load_Micro_Chunks (ChunkLoadClass &cload);

private:

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
};


#endif //__EDITOR_SAVE_LOAD_H
