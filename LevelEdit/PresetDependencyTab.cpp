// PresetGeneralTab.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "PresetDependencyTab.h"
#include "definition.h"
#include "preset.h"
#include "utils.h"
#include "filemgr.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
//
// PresetDependencyTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetDependencyTabClass::PresetDependencyTabClass (PresetClass *preset)
	:	m_Preset (preset),
		m_IsReadOnly (false),
		DockableFormClass(PresetDependencyTabClass::IDD)
{
	//{{AFX_DATA_INIT(PresetDependencyTabClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// ~PresetDependencyTabClass
//
/////////////////////////////////////////////////////////////////////////////
PresetDependencyTabClass::~PresetDependencyTabClass (void)
{
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::DoDataExchange (CDataExchange *pDX)
{
	DockableFormClass::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(PresetDependencyTabClass)
	DDX_Control(pDX, IDC_DEPENCY_LIST, m_ListCtrl);
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(PresetDependencyTabClass, DockableFormClass)
	//{{AFX_MSG_MAP(PresetDependencyTabClass)
	ON_BN_CLICKED(IDC_ADD, OnAdd)
	ON_BN_CLICKED(IDC_REMOVE, OnRemove)
	ON_NOTIFY(LVN_ITEMCHANGED, IDC_DEPENCY_LIST, OnItemchangedDepencyList)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// PresetDependencyTabClass diagnostics

#ifdef _DEBUG
void PresetDependencyTabClass::AssertValid() const
{
	DockableFormClass::AssertValid();
}

void PresetDependencyTabClass::Dump(CDumpContext& dc) const
{
	DockableFormClass::Dump(dc);
}
#endif //_DEBUG


/////////////////////////////////////////////////////////////////////////////
//
// HandleInitDialog
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::HandleInitDialog (void)
{
	ASSERT (m_Preset != NULL);

	//
	//	Configure the list control
	//
	m_ListCtrl.InsertColumn (0, "Filename");
	
	CRect rect;
	m_ListCtrl.GetClientRect (&rect);
	m_ListCtrl.SetColumnWidth (0, rect.Width () - 4);
	
	//
	//	Get the list of dependencies from the preset
	//
	STRING_LIST dependencies;
	m_Preset->Get_Manual_Dependencies (dependencies);

	//
	//	Add the dependencies to the list control
	//
	for (int index = 0; index < dependencies.Count (); index ++) {
		m_ListCtrl.InsertItem (index, dependencies[index]);
	}

	//
	//	Make the controls read-only if necessary
	//
	if (m_IsReadOnly) {
		::EnableWindow (::GetDlgItem (m_hWnd, IDC_ADD), false);
		::EnableWindow (::GetDlgItem (m_hWnd, IDC_REMOVE), false);
		::EnableWindow (::GetDlgItem (m_hWnd, IDC_DEPENCY_LIST), false);
	}

	//
	// Put the focus into the edit control
	//
	::SetFocus (::GetDlgItem (m_hWnd, IDC_DEPENCY_LIST));
	Update_Button_State ();	
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Apply_Changes
//
/////////////////////////////////////////////////////////////////////////////
bool
PresetDependencyTabClass::Apply_Changes (void)
{
	//
	//	Loop over all the filenames in the list control
	//
	STRING_LIST list;
	for (int index = 0; index < m_ListCtrl.GetItemCount (); index ++) {
		
		//
		//	Add this file to the list of dependencies
		//
		CString filename = m_ListCtrl.GetItemText (index, 0);
		list.Add (filename);
	}

	//
	//	Pass the new dependency list onto the preset
	//
	m_Preset->Set_Manual_Dependencies (list);	
	return true;
}


/////////////////////////////////////////////////////////////////////////////
//
// Update_Button_State
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::Update_Button_State (void)
{
	int sel_index = m_ListCtrl.GetNextItem (-1, LVNI_SELECTED | LVNI_ALL);

	if (m_IsReadOnly == false) {	
		::EnableWindow (::GetDlgItem (m_hWnd, IDC_REMOVE), bool(sel_index >= 0));
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnAdd
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::OnAdd (void)
{
	CFileDialog dialog (TRUE,
							  ".w3d",
							  NULL,
							  OFN_HIDEREADONLY | OFN_OVERWRITEPROMPT | OFN_EXPLORER,
							  "All Files|*.*|Westwood 3D Files|*.w3d|Texture Files|*.tga|Sound Files|*.wav;*.mp3||",
							  this);
	
	//
	//	Setup the initial directory
	//
	dialog.m_ofn.lpstrInitialDir = ::Get_File_Mgr ()->Get_Base_Path ();

	//
	//	Let the user pick a file
	//
	if (dialog.DoModal () == IDOK) {
		
		//
		// Is the new path valid?
		//
		if (::Get_File_Mgr ()->Is_Path_Valid (dialog.GetPathName ())) {

			//
			// Convert the new asset file path to a relative path and pass it onto the base
			//
			CString rel_path = ::Get_File_Mgr ()->Make_Relative_Path (dialog.GetPathName ());
			m_ListCtrl.InsertItem (0xFF, rel_path);

		} else {
			
			//
			// Let the user know this path is invalid
			//
			CString message;
			CString title;
			message.Format (IDS_INVALID_MODEL_PATH_MSG, (LPCTSTR)::Get_File_Mgr()->Get_Base_Path ());
			title.LoadString (IDS_INVALID_MODEL_PATH_TITLE);			
			::MessageBox (m_hWnd, message, title, MB_ICONERROR | MB_OK);
		}		

	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnRemove
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::OnRemove (void)
{
	//
	//	Get the index of the currently selected entry
	//
	int sel_index = m_ListCtrl.GetNextItem (-1, LVNI_SELECTED | LVNI_ALL);
	if (sel_index >= 0) {
		
		//
		//	Remove this entry from the list control
		//
		m_ListCtrl.DeleteItem (sel_index);
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnItemchangedDepencyList
//
/////////////////////////////////////////////////////////////////////////////
void
PresetDependencyTabClass::OnItemchangedDepencyList
(
	NMHDR *	pNMHDR,
	LRESULT* pResult
)
{
	NM_LISTVIEW* pNMListView = (NM_LISTVIEW*)pNMHDR;
	(*pResult) = 0;

	Update_Button_State ();	
	return ;
}
