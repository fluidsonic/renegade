// GenericTextDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "GenericTextDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
//	GenericTextDialogClass
//
/////////////////////////////////////////////////////////////////////////////
GenericTextDialogClass::GenericTextDialogClass (CWnd *pParent /*=NULL*/)
	:	m_IconID (0),
		m_Title ("Information"),
		CDialog(GenericTextDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(GenericTextDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
//	DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
GenericTextDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(GenericTextDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(GenericTextDialogClass, CDialog)
	//{{AFX_MSG_MAP(GenericTextDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
//	OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
GenericTextDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	//
	//	Set the text properties of the dialog
	//
	SetWindowText (m_Title);
	SetDlgItemText (IDC_TEXT, m_Text);
	SetDlgItemText (IDC_DESCRIPTION, m_Description);

	//
	//	Load the icon
	//
	HICON icon = NULL;
	if (m_IconID == 0) {		
		icon = ::LoadIcon (NULL, IDI_INFORMATION);
	} else {
		icon = ::LoadIcon (::AfxGetResourceHandle (), MAKEINTRESOURCE (m_IconID));
	}

	//
	//	Set the icon
	//
	if (icon != NULL) {
		SendDlgItemMessage (IDC_GENERIC_ICON, STM_SETIMAGE, (WPARAM)IMAGE_ICON, (LPARAM)icon);
	}

	return TRUE;
}
