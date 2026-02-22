#pragma once

#include "global.h"

#include "inputconfig.h"
#include "vector.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class ChunkSaveClass;
class ChunkLoadClass;

////////////////////////////////////////////////////////////////
//
//	InputConfigMgrClass
//
////////////////////////////////////////////////////////////////
class InputConfigMgrClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	static void		Initialize (void);
	static void		Shutdown (void);

	//
	//	Save/load support
	//
	static void		Save (void);
	static void		Load (void);

	//
	//	Current configuration support
	//
	static bool		Get_Current_Configuration (InputConfigClass &config);
	static int		Get_Current_Configuration_Index (void)							{ return CurrentConfigIndex; }
	static void		Load_Current_Configuration (void);
	static void		Load_Default_Configuration (void);
	static void		Load_Configuration (const InputConfigClass &config);
	static void		Save_Current_Configuration (void);	
	static void		Save_Configuration (const InputConfigClass &config);

	//
	//	Configuration list support
	//
	static int		Add_Configuration (const WCHAR *display_name);
	static void		Delete_Configuration (const char *filename);
	static void		Delete_Configuration (int index);
	

	//
	//	Configuration list access
	//
	static int		Get_Configuration_Count (void)									{ return ConfigList.Count (); }
	static void		Get_Configuration (int index, InputConfigClass &config)	{ config = ConfigList[index]; }	

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	static int		Find_Configuration (const char *filename);
	static void		Get_Unique_Config_Filename (StringClass &filename);
	static void		Get_Config_Path (StringClass &full_path);

	//
	//	Save/load support
	//
	static void		Save_Variables (ChunkSaveClass &csave);
	static void		Load_Variables (ChunkLoadClass &cload);

	static void		Save_Config_List (ChunkSaveClass &csave);
	static void		Load_Config_List (ChunkLoadClass &cload);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	static DynamicVectorClass<InputConfigClass>	ConfigList;
	static int												CurrentConfigIndex;
};
