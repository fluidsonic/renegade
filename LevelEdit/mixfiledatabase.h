#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __MIXFILEDATABASE_H
#define __MIXFILEDATABASE_H


#include "assetdatabase.h"
#include "wwstring.h"
#include "ffactory.h"
#include "ffactorylist.h"


//////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////
//
//	MixFileDatabaseClass
//
//////////////////////////////////////////////////////////////////////
class MixFileDatabaseClass : public AssetDatabaseClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	MixFileDatabaseClass (void);
	virtual ~MixFileDatabaseClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////
	
	//
	//	Database methods
	//
	bool				Open_Database (LPCTSTR ini_filename, LPCTSTR username = NULL, LPCTSTR password = NULL);
	
	//
	//	File manipulation methods
	//
	bool				Add_File (LPCTSTR local_filename, LPCTSTR comment = NULL)	{ return true; }
	bool				Check_In (LPCTSTR local_filename, LPCTSTR comment = NULL)	{ return true; }
	bool				Check_Out (LPCTSTR local_filename, bool get_locally = true);
	bool				Undo_Check_Out (LPCTSTR local_filename)							{ return true; }
	bool				Get (LPCTSTR local_filename);
	bool				Get_Subproject (LPCTSTR local_filename)							{ return true; }
	bool				Get_All (LPCTSTR dest_path, LPCTSTR search_mask);

	FileClass *		Get_File (LPCTSTR local_filename);

	void				Find_Files (DynamicVectorClass<StringClass> &file_list, LPCTSTR search_mask);

	//
	//	Extended methods which provide UI
	//
	bool				Check_Out_Ex (LPCTSTR local_filename, HWND parent_wnd)	{ return true; }
	bool				Check_In_Ex (LPCTSTR local_filename, HWND parent_wnd)		{ return true; }

	//
	// Retry methods
	//
	bool				Retry_Check_Out (LPCTSTR local_filename, int attempts = 1, int delay = 250)	{ return true; }
	bool				Retry_Check_In (LPCTSTR local_filename, int attempts = 1, int delay = 250)		{ return true; }
	
	//
	//	File information methods
	//		
	FILE_STATUS		Get_File_Status (LPCTSTR local_filename, StringClass *checked_out_user_name = NULL) { return CHECKED_OUT_TO_ME; }
	bool				Is_File_Different (LPCTSTR local_filename) { return (Does_File_Exist (local_filename) == false); }
	bool				Does_File_Exist (LPCTSTR local_filename);

	//
	// User information
	//
	BOOL				Is_Read_Only (void) const	{ return FALSE; }

	//
	//	Misc information
	//
	const char *	Get_Mix_File_Path (void) const	{ return MixFilePath; }

	//
	//	Static methods
	//
	static MixFileDatabaseClass *	Get_Instance (void)	{ return _TheInstance; }

protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	bool				Copy_File (FileClass *file, LPCTSTR local_filename);
	void				Create_Directory_Structure (LPCTSTR path);	
	bool				Internal_Does_File_Exist (LPCSTR filename);
	bool				Internal_Get (LPCSTR filename, LPCSTR local_path);
	bool				Is_Texture (LPCSTR filename);
	void				Swap_Texture_Extension (StringClass &filename);

	void				Get_Filename (LPCTSTR path, StringClass &filename);

	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////	
	StringClass		ModName;
	StringClass		ModAssetPath;
	StringClass		MixFilePath;

	FileFactoryListClass		MainFileFactory;
	SimpleFileFactoryClass	RenegadeDataFileFactory;

	static MixFileDatabaseClass *	_TheInstance;
};


#endif //__MIXFILEDATABASE_H
