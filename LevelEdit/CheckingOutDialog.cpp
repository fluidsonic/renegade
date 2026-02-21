// CheckingOutDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "CheckingOutDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CheckingOutDialogClass dialog


CheckingOutDialogClass::CheckingOutDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(CheckingOutDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(CheckingOutDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}


void CheckingOutDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CheckingOutDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CheckingOutDialogClass, CDialog)
	//{{AFX_MSG_MAP(CheckingOutDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// WindowProc
//
LRESULT
CheckingOutDialogClass::WindowProc
(
	UINT message,
	WPARAM wParam,
	LPARAM lParam
) 
{
	if (message == WM_USER+101) {
		EndDialog (1);
	}
	
	// Allow the base class to process this message	
	return CDialog::WindowProc(message, wParam, lParam);
}


/////////////////////////////////////////////////////////////////////////////
//
// OnCommand
//
BOOL
CheckingOutDialogClass::OnCommand
(
	WPARAM wParam,
	LPARAM lParam
) 
{
	// Don't let the dialog be closed by hitting enter or escape
	if (LOWORD (wParam) == IDOK || LOWORD (wParam) == IDCANCEL) {
		return FALSE;
	}
	
	// Allow the base class to process this message	
	return CDialog::OnCommand(wParam, lParam);
}


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
BOOL
CheckingOutDialogClass::OnInitDialog (void) 
{
	// Allow the base class to process this message	
	CDialog::OnInitDialog ();
		
	return TRUE;
}
