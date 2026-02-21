#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __STRINGSMGR_H
#define __STRINGSMGR_H

#include "bittype.h"
#include "widestring.h"


/////////////////////////////////////////////////////////////////////////
//
//	StringsMgrClass
//
/////////////////////////////////////////////////////////////////////////
class StringsMgrClass
{
	public:

		//////////////////////////////////////////////////////////
		//	Public methods
		//////////////////////////////////////////////////////////

		//
		//	Translation database save/load
		//
		static void		Save_Translation_Database (void);
		static void		Save_Translation_Database (const char *full_path);
		static void		Load_Translation_Database (void);
		static void		Create_Database_If_Necessary (void);

		//
		//	Translation database version control
		//
		static bool		Get_Latest_Version (void);
		static bool		Check_Out (void);
		static bool		Check_In (void);
		static bool		Undo_Check_Out (void);

		//
		//	Translation database ID import/export
		//
		static void		Import_Strings (void);
		static void		Import_IDs (void);
		static void		Export_IDs (void);

		//
		//	Translation database import/export
		//
		static void		Export_For_Translation (const char *filename, uint32 lang_id);
		static void		Import_From_Translation (const char *filename, uint32 lang_id);

		//
		//	Editor support
		//
		static void		Edit_Database (HWND parent_wnd);

	protected:

		//////////////////////////////////////////////////////////
		//	Protected methods
		//////////////////////////////////////////////////////////

		static void		Convert_Newline_To_Chars (WideStringClass &string);
		static void		Convert_Chars_To_Newline (WideStringClass &string);
		static void		Apply_Characteristics (WideStringClass &english_string, WideStringClass &translated_string);
		
	private:

		//////////////////////////////////////////////////////////
		//	Private member data
		//////////////////////////////////////////////////////////
};


#endif //__STRINGSMGR_H
