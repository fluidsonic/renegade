#include "stdafx.h"
#include "conversationeditormgr.h"
#include "filelocations.h"
#include "filemgr.h"
#include "assetdatabase.h"
#include "chunkio.h"
#include "rawfile.h"
#include "saveload.h"
#include "translatedb.h"
#include "conversationmgr.h"
#include "conversationpage.h"
#include "utils.h"


/////////////////////////////////////////////////////////////////////////
//
//	Create_Database_If_Necessary
//
/////////////////////////////////////////////////////////////////////////
void
ConversationEditorMgrClass::Create_Database_If_Necessary (void)
{
	FileMgrClass *file_mgr			= ::Get_File_Mgr ();
	AssetDatabaseClass &asset_db	= file_mgr->Get_Database_Interface ();
	
	//
	//	Determine where the file should exist locally
	//
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);

	//
	//	Check to see if the file exists in VSS
	//
	if (asset_db.Does_File_Exist (filename) == false) {
		
		//
		//	Save a copy of the database to disk and add it to VSS
		//
		Save_Global_Database ();
		asset_db.Add_File (filename);
	} else {
		
		//
		//	The file exists in VSS, so update our local copy
		//
		Get_Latest_Version ();
	}
	
	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	Save_Global_Database
//
/////////////////////////////////////////////////////////////////////////
void
ConversationEditorMgrClass::Save_Global_Database (void)
{
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);

	//
	//	Create the file
	//
	HANDLE file = ::CreateFile (filename, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS,
							0L, NULL);

	ASSERT (file != INVALID_HANDLE_VALUE);
	if (file != INVALID_HANDLE_VALUE) {

		RawFileClass file_obj;
		file_obj.Attach (file);
		ChunkSaveClass chunk_save (&file_obj);

		//
		//	Save the conversation database subsystem
		//
		_ConversationMgrSaveLoad.Set_Category_To_Save (ConversationMgrClass::CATEGORY_GLOBAL);
		SaveLoadSystemClass::Save (chunk_save, _ConversationMgrSaveLoad);
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	Load_Global_Database
//
/////////////////////////////////////////////////////////////////////////
void
ConversationEditorMgrClass::Load_Global_Database (void)
{
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);

	//
	//	Open the file
	//
	HANDLE file = ::CreateFile (filename, GENERIC_READ, FILE_SHARE_READ, NULL,
							OPEN_EXISTING, 0L, NULL);

	ASSERT (file != INVALID_HANDLE_VALUE);
	if (file != INVALID_HANDLE_VALUE) {

		RawFileClass file_obj;
		file_obj.Attach (file);
		ChunkLoadClass chunk_load (&file_obj);

		//
		//	Let the save/load system handle the laod
		//
		SaveLoadSystemClass::Load (chunk_load);

		//
		//	Update the UI
		//
		ConversationPageClass *conversation_form = ::Get_Conversation_Form ();
		if (conversation_form) {
			conversation_form->Reload_Data ();
		}
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////
//
//	Get_Latest_Version
//
/////////////////////////////////////////////////////////////////////////
bool
ConversationEditorMgrClass::Get_Latest_Version (void)
{
	FileMgrClass *file_mgr			= ::Get_File_Mgr ();
	AssetDatabaseClass &asset_db	= file_mgr->Get_Database_Interface ();
	
	//
	//	Determine where the file should exist locally
	//
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);
	
	//
	//	Ask VSS to get the latest version of the file for us
	//
	return asset_db.Get (filename);
}


/////////////////////////////////////////////////////////////////////////
//
//	Check_Out
//
/////////////////////////////////////////////////////////////////////////
bool
ConversationEditorMgrClass::Check_Out (void)
{
	FileMgrClass *file_mgr			= ::Get_File_Mgr ();
	AssetDatabaseClass &asset_db	= file_mgr->Get_Database_Interface ();
	
	//
	//	Determine where the file should exist locally
	//
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);
	
	bool retval = true;
	if (asset_db.Does_File_Exist (filename)) {
	
		//
		//	Ask VSS to check out the file to us
		//
		retval = asset_db.Check_Out_Ex (filename, ::AfxGetMainWnd ()->m_hWnd);
	}
	
	return retval;
}


/////////////////////////////////////////////////////////////////////////
//
//	Check_In
//
/////////////////////////////////////////////////////////////////////////
bool
ConversationEditorMgrClass::Check_In (void)
{
	FileMgrClass *file_mgr			= ::Get_File_Mgr ();
	AssetDatabaseClass &asset_db	= file_mgr->Get_Database_Interface ();
	
	//
	//	Determine where the file should exist locally
	//
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);
	
	//
	//	Ask VSS to check in the file for us
	//
	return asset_db.Check_In_Ex (filename, ::AfxGetMainWnd ()->m_hWnd);
}


/////////////////////////////////////////////////////////////////////////
//
//	Undo_Check_Out
//
/////////////////////////////////////////////////////////////////////////
bool
ConversationEditorMgrClass::Undo_Check_Out (void)
{
	FileMgrClass *file_mgr			= ::Get_File_Mgr ();
	AssetDatabaseClass &asset_db	= file_mgr->Get_Database_Interface ();
	bool retval							= false;

	//
	//	Determine where the file should exist locally
	//
	CString filename = ::Get_File_Mgr ()->Make_Full_Path (CONV_DB_PATH);

	//
	//	We only undo the checkout if its checked out to us
	//
	if (asset_db.Get_File_Status (filename) == AssetDatabaseClass::CHECKED_OUT_TO_ME) {
		retval = asset_db.Undo_Check_Out (filename);
	}

	return retval;
}

