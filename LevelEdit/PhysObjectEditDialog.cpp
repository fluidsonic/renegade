#include "stdafx.h"
#include "leveledit.h"
#include "PhysObjectEditDialog.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// PhysObjectEditDialogClass
//
/////////////////////////////////////////////////////////////////////////////
PhysObjectEditDialogClass::PhysObjectEditDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(PhysObjectEditDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(PhysObjectEditDialogClass)
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
PhysObjectEditDialogClass::DoDataExchange (CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(PhysObjectEditDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(PhysObjectEditDialogClass, CDialog)
	//{{AFX_MSG_MAP(PhysObjectEditDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
PhysObjectEditDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	CRect rect;
	::GetWindowRect (::GetDlgItem (m_hWnd, IDC_EDIT_AREA), &rect);
	ScreenToClient (&rect);
	::DestroyWindow (::GetDlgItem (m_hWnd, IDC_EDIT_AREA));

	//
	//	Setup the form we use to edit the physics object
	//
	m_ObjectEditForm.Create (this, 101);
	m_ObjectEditForm.SetWindowPos (NULL, rect.left, rect.top, rect.Width (), rect.Height (), 0);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
PhysObjectEditDialogClass::OnOK (void) 
{
	if (m_ObjectEditForm.Apply_Changes ()) {
		CDialog::OnOK ();
	}

	return ;
}
