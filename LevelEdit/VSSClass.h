#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __VSSCLASS_H
#define __VSSCLASS_H


#include "wtypes.h"
#include "oaidl.h"
#include "ocidl.h"
#include "vss.h"
#include "utils.h"
#include "assetdatabase.h"


///////////////////////////////////////////////////////////////////////////
//
// VSSClass
//
//	Class definition used to abstract the Visual Source Safe COM interface.
//
///////////////////////////////////////////////////////////////////////////
class VSSClass : public AssetDatabaseClass
{
	public:

		//////////////////////////////////////////////////////////
		//
		//	Public constructors/destructors
		//
		VSSClass (void)
			:	m_pIVSSDatabase (NULL),
				m_bReadOnly (TRUE)				{ ::CoInitialize (NULL); }
		virtual ~VSSClass (void)				{ COM_RELEASE (m_pIVSSDatabase); ::CoUninitialize (); }


		//////////////////////////////////////////////////////////
		//
		//	Public Methods
		//
		
		//
		//	Database methods
		//
		bool						Open_Database (LPCTSTR ini_filename, LPCTSTR username = NULL, LPCTSTR password = NULL);
		
		//
		//	File manipulation methods
		//
		bool						Add_File (LPCTSTR local_filename, LPCTSTR comment = NULL);
		bool						Check_In (LPCTSTR local_filename, LPCTSTR comment = NULL);
		bool						Check_Out (LPCTSTR local_filename, bool get_locally = true);
		bool						Undo_Check_Out (LPCTSTR local_filename);
		bool						Get (LPCTSTR local_filename);
		bool						Get (LPCTSTR local_filename, IVSSItem *pitem);
		void						Get_Recursive (LPCTSTR local_path, IVSSItem *pparent);
		bool						Get_Subproject (LPCTSTR local_filename);		
		bool						Destroy (LPCTSTR local_filename);

		FileClass *				Get_File (LPCTSTR local_filename);

		//
		//	Extended methods which provide UI
		//
		bool						Check_Out_Ex (LPCTSTR local_filename, HWND parent_wnd);
		bool						Check_In_Ex (LPCTSTR local_filename, HWND parent_wnd);

		//
		// Retry methods
		//
		bool						Retry_Check_Out (LPCTSTR local_filename, int iattempts = 1, int idelay = 250);
		bool						Retry_Check_In (LPCTSTR local_filename, int iattempts = 1, int idelay = 250);

		//
		//	Folder (subproject) methods
		//
		bool						Build_Tree (LPCTSTR local_filename);
		
		//
		//	File information methods
		//
		
		VSSFileStatus			Get_File_Status (LPCTSTR local_filename, LPTSTR checked_out_username = NULL, DWORD buffer_size = 0, IVSSItem *item_to_use = NULL);
		FILE_STATUS				Get_File_Status (LPCTSTR local_filename, StringClass *checked_out_user_name);

		long						Get_File_Version (LPCTSTR local_filename);
		bool						Is_File_Different (LPCTSTR local_filename);
		bool						Does_File_Exist (LPCTSTR local_filename);
		bool						Get_File_Date (IVSSItem *pitem, SYSTEMTIME &system_time);
		VSSItemType				Get_Type (LPCTSTR local_filename);

		//
		// User information
		//
		BOOL						Is_Read_Only (void) const	{ return m_bReadOnly; }


	protected:
		
		//////////////////////////////////////////////////////////
		//
		//	Protected methods
		//
		void						Get_VSS_Interface (void);
		IVSSItem *				Get_VSS_Item_From_Local_Path (LPCTSTR local_filename);
		IVSSItem *				Get_VSS_Item (LPCTSTR vss_path);
		LPCTSTR					Get_Error_Description (HRESULT hresult);

		
	private:

		//////////////////////////////////////////////////////////
		//
		//	Private member data
		//
		IVSSDatabase *		m_pIVSSDatabase;
		BOOL					m_bReadOnly;
};

#endif //__VSSCLASS_H

