// GotoObjectByIDDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"

#include "gotoobjectbyiddialog.h"
#include "node.h"
#include "nodemgr.h"
#include "cameramgr.h"
#include "utils.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// GotoObjectByIDDialogClass
//
/////////////////////////////////////////////////////////////////////////////
GotoObjectByIDDialogClass::GotoObjectByIDDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(GotoObjectByIDDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(GotoObjectByIDDialogClass)
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
GotoObjectByIDDialogClass::DoDataExchange (CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(GotoObjectByIDDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(GotoObjectByIDDialogClass, CDialog)
	//{{AFX_MSG_MAP(GotoObjectByIDDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
GotoObjectByIDDialogClass::OnOK (void)
{
	int obj_id = GetDlgItemInt (IDC_OBJECT_ID_EDIT);

	//
	//	Try to find the object that the user entered
	//
	NodeClass *node = NodeMgrClass::Find_Node (obj_id);
	if (node == NULL) {
		
		//
		//	Warn the user
		//
		::MessageBox (m_hWnd, "There is not object in the current level with that ID.", "Object Not Found", MB_ICONERROR | MB_OK);
		
		//
		//	Return focus to the edit control so the user can re-enter the ID
		//
		::SetFocus (::GetDlgItem (m_hWnd, IDC_OBJECT_ID_EDIT));
		SendDlgItemMessage (IDC_OBJECT_ID_EDIT, EM_SETSEL, (WPARAM)0, (LPARAM)-1);
	} else {

		//
		//	'Goto' the requested object
		//
		::Get_Camera_Mgr ()->Goto_Node (node);

		CDialog::OnOK ();
	}
	
	return ;
}
