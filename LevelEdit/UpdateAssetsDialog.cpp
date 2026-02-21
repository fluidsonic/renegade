#include "StdAfx.H"
#include "LevelEdit.H"
#include "UpdateAssetsDialog.H"
#include "FileMgr.H"
#include "Utils.H"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// UpdateAssetsDialogClass
//
/////////////////////////////////////////////////////////////////////////////
UpdateAssetsDialogClass::UpdateAssetsDialogClass
(
	const CString &	comments,
	STRING_LIST &		directory_list,
	bool					update_all,
	CWnd *				pParent
)
	:	m_Comments (comments),
		m_DirectoryList (directory_list),
		m_UpdateAll (update_all),
		CDialog(UpdateAssetsDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(UpdateAssetsDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
UpdateAssetsDialogClass::DoDataExchange (CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(UpdateAssetsDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(UpdateAssetsDialogClass, CDialog)
	//{{AFX_MSG_MAP(UpdateAssetsDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
UpdateAssetsDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	//
	//	Force the window on top
	//
	BringWindowToTop ();
	SetForegroundWindow ();	
	SetWindowPos (&wndTopMost, 0, 0, 0, 0, SWP_NOSIZE | SWP_NOMOVE);

	// Pass the comments onto the edit control
	SetDlgItemText (IDC_COMMENTS_EDIT, m_Comments);

	// Check the 'Yes' radio button by default
	SendDlgItemMessage (IDC_YES_RADIO, BM_SETCHECK, (WPARAM)TRUE);

	//
	//	Simulate pressing the OK button
	//
	if (::Is_Silent_Mode ()) {
		PostMessage (WM_COMMAND, MAKELONG (IDOK, BN_CLICKED), 0L);
	}

	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
UpdateAssetsDialogClass::OnOK (void) 
{
	// Should we update the assets now?
	if (SendDlgItemMessage (IDC_YES_RADIO, BM_GETCHECK) == 1) {
		
		//
		// Put up a dialog while we copy all the assets from VSS
		//
		HWND hdlg = Show_VSS_Update_Dialog (m_hWnd);
		
		if (m_UpdateAll) {
			::Get_File_Mgr ()->Update_Asset_Tree ();
		} else {
			
			//
			//	Loop over the directory list and only update those directories that
			// are new...
			//
			for (int index = 0; index < m_DirectoryList.Count (); index ++) {
				CString &path = m_DirectoryList[index];
				::Get_File_Mgr ()->Get_Subproject (path);
			}
		}

		Kill_VSS_Update_Dialog (hdlg);
		CDialog::OnOK ();
	} else {
		CDialog::OnCancel ();
	}

	return ;
}
