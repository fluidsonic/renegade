// FilePickerClass.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "filepicker.h"
#include "utils.h"
#include "filemgr.h"


///////////////////////////////////////////////////////////
//
//	FilePickerClass
//
///////////////////////////////////////////////////////////
FilePickerClass::FilePickerClass (void)
	: m_AssetTreeOnly (true),
	  PickerClass ()
{
	m_ExtensionString = "*.*";
	m_FilterString = "All Files (*.*)|*.*||";
	return ;
}


///////////////////////////////////////////////////////////
//
//	~FilePickerClass
//
///////////////////////////////////////////////////////////
FilePickerClass::~FilePickerClass (void)
{
	return ;
}


///////////////////////////////////////////////////////////
//
//	On_Pick
//
///////////////////////////////////////////////////////////
void
FilePickerClass::On_Pick (void)
{
	CString window_text;
	GetWindowText (window_text);
	CString full_path	= ::Get_File_Mgr ()->Make_Full_Path (window_text);
	CString filename	= ::Get_Filename_From_Path (full_path);
	CString path;
	
	DWORD attribs = ::GetFileAttributes (full_path);
	if ((attribs != 0xFFFFFFFF) && !(attribs & FILE_ATTRIBUTE_DIRECTORY)) {
		path = ::Strip_Filename_From_Path (full_path);
	} else if ((attribs == 0xFFFFFFFF) && (::strchr (filename, '*') != NULL)) {
		path = ::Strip_Filename_From_Path (full_path);
	} else if ((attribs == 0xFFFFFFFF) && m_AssetTreeOnly) {
		path = ::Get_File_Mgr ()->Get_Base_Path ();
	} else {
		path = full_path;
	}

	CFileDialog dialog (TRUE,
							  m_ExtensionString,
							  filename,
							  OFN_HIDEREADONLY | OFN_OVERWRITEPROMPT | OFN_EXPLORER,
							  m_FilterString,
							  this);

	//
	// Set the path, so it opens in the correct directory
	//
	if (path.GetLength () > 0) {
		dialog.m_ofn.lpstrInitialDir = path;
	}

	//
	// Show the dialog to the user
	//
	if (dialog.DoModal () == IDOK) {

		path = dialog.GetPathName ();
		if (::Get_File_Mgr ()->Is_Empty_Path (path) == false)
		{
			if (m_AssetTreeOnly == false || ::Get_File_Mgr ()->Is_Path_Valid (path)) {
				CString rel_path = ::Get_File_Mgr ()->Make_Relative_Path (dialog.GetPathName ());
				SetWindowText (rel_path);
			}
		}
	}		

	PickerClass::On_Pick ();
	return ;
}

