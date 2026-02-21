#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __ASSETDATABASE_H
#define __ASSETDATABASE_H


//////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////
class StringClass;
class FileClass;


//////////////////////////////////////////////////////////////////////
//
//	AssetDatabaseClass
//
//////////////////////////////////////////////////////////////////////
class AssetDatabaseClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public data types
	///////////////////////////////////////////////////////////////////
	typedef enum
	{
		UNKNOWN					= -1,
		CHECKED_OUT_TO_ME		= 0,
		CHECKED_OUT,
		NOT_CHECKED_OUT

	} FILE_STATUS;

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	AssetDatabaseClass  (void)					{}
	virtual ~AssetDatabaseClass (void)		{}

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////
	
		//
		//	Database methods
		//
		virtual bool				Open_Database (LPCTSTR ini_filename, LPCTSTR username = NULL, LPCTSTR password = NULL) = 0;
		
		//
		//	File manipulation methods
		//
		virtual bool				Add_File (LPCTSTR local_filename, LPCTSTR comment = NULL) = 0;
		virtual bool				Check_In (LPCTSTR local_filename, LPCTSTR comment = NULL) = 0;
		virtual bool				Check_Out (LPCTSTR local_filename, bool get_locally = true) = 0;
		virtual bool				Undo_Check_Out (LPCTSTR local_filename) = 0;
		virtual bool				Get (LPCTSTR local_filename) = 0;
		virtual bool				Get_Subproject (LPCTSTR local_filename) = 0;
		virtual bool				Get_All (LPCTSTR dest_path, LPCTSTR search_mask) { return false; }

		virtual FileClass *		Get_File (LPCTSTR local_filename) = 0;

		//
		//	Extended methods which provide UI
		//
		virtual bool				Check_Out_Ex (LPCTSTR local_filename, HWND parent_wnd) = 0;
		virtual bool				Check_In_Ex (LPCTSTR local_filename, HWND parent_wnd) = 0;

		//
		// Retry methods
		//
		virtual bool				Retry_Check_Out (LPCTSTR local_filename, int attempts = 1, int delay = 250) = 0;
		virtual bool				Retry_Check_In (LPCTSTR local_filename, int attempts = 1, int delay = 250) = 0;
		
		//
		//	File information methods
		//
		
		virtual FILE_STATUS		Get_File_Status (LPCTSTR local_filename, StringClass *checked_out_user_name = NULL) = 0;
		virtual bool				Is_File_Different (LPCTSTR local_filename) = 0;
		virtual bool				Does_File_Exist (LPCTSTR local_filename) = 0;

		//
		// User information
		//
		virtual BOOL				Is_Read_Only (void) const	{ return TRUE; }

protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////	
};


#endif //__ASSETDATABASE_H
