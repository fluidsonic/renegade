// StringsCategoryNameDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "StringsCategoryNameDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
//
// StringsCategoryNameDialogClass
//
/////////////////////////////////////////////////////////////////////////////
StringsCategoryNameDialogClass::StringsCategoryNameDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(StringsCategoryNameDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(StringsCategoryNameDialogClass)
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
StringsCategoryNameDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(StringsCategoryNameDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(StringsCategoryNameDialogClass, CDialog)
	//{{AFX_MSG_MAP(StringsCategoryNameDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
StringsCategoryNameDialogClass::OnInitDialog (void) 
{
	CDialog::OnInitDialog ();

	SetDlgItemText (IDC_NAME, Name);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
StringsCategoryNameDialogClass::OnOK (void)
{
	GetDlgItemText (IDC_NAME, Name);
	CDialog::OnOK ();
	return ;
}

