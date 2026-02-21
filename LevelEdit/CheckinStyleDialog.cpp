// CheckinStyleDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "CheckinStyleDialog.h"
#include "RegKeys.H"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// Constants
//
const TCHAR * const REG_NOT_CACHED		= TEXT ("NotCached");
const TCHAR * const REG_NOW				= TEXT ("Now");
const TCHAR * const REG_LATER				= TEXT ("Later");


/////////////////////////////////////////////////////////////////////////////
//
// CheckinStyleDialogClass
//
CheckinStyleDialogClass::CheckinStyleDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(CheckinStyleDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(CheckinStyleDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}


void CheckinStyleDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CheckinStyleDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CheckinStyleDialogClass, CDialog)
	//{{AFX_MSG_MAP(CheckinStyleDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CheckinStyleDialogClass message handlers


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
void
CheckinStyleDialogClass::OnOK (void)
{
	UINT ret_code = IDC_UPDATE_NOW;
	CString cache_value = REG_NOW;

	if (SendDlgItemMessage (IDC_UPDATE_LATER, BM_GETCHECK) == 1) {
		ret_code = IDC_UPDATE_LATER;
		cache_value = REG_LATER;
	}

	// Cache the value in the registry if necessary
	if (SendDlgItemMessage (IDC_DONT_ASK_ME_AGAIN, BM_GETCHECK) == 1) {
		theApp.WriteProfileString (CONFIG_KEY, CHECKIN_STYLE_VALUE, cache_value);
	}

	// Close the dialog with the appropriate control ID
	EndDialog (ret_code);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnCommand
//
BOOL
CheckinStyleDialogClass::OnCommand
(
	WPARAM wParam,
	LPARAM lParam
)
{
	// Don't allow the user to cancel out of this dialog
	if (LOWORD (wParam) == IDCANCEL) {
		return FALSE;
	}

	// Allow the base class to process this message
	return CDialog::OnCommand (wParam, lParam);
}


/////////////////////////////////////////////////////////////////////////////
//
// DoModal
//
int
CheckinStyleDialogClass::DoModal (void)
{	
	UINT ret_code = IDC_UPDATE_NOW;

	// Get the cached 'checkin' style if possible
	CString checkin_style = theApp.GetProfileString (CONFIG_KEY, CHECKIN_STYLE_VALUE, REG_NOT_CACHED);
	if (checkin_style.CompareNoCase (REG_NOT_CACHED) == 0) {
		
		// Allow the base class to show the dialog
		ret_code = CDialog::DoModal ();
	} else {
		if (checkin_style.CompareNoCase (REG_LATER) == 0) {
			ret_code = IDC_UPDATE_LATER;
		}		
	}

	// Return the dialog control's ID	
	return ret_code;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
BOOL
CheckinStyleDialogClass::OnInitDialog (void) 
{
	// Allow the base class to process this message
	CDialog::OnInitDialog ();

	// Check 'update now' by default
	SendDlgItemMessage (IDC_UPDATE_NOW, BM_SETCHECK, (WPARAM)TRUE);
	return TRUE;
}
