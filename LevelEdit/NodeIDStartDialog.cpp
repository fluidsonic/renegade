// NodeIDStartDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "NodeIDStartDialog.h"
#include "regkeys.h"
#include "utils.h"
#include "nodemgr.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// NodeIDStartDialogClass
//
/////////////////////////////////////////////////////////////////////////////
NodeIDStartDialogClass::NodeIDStartDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(NodeIDStartDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(NodeIDStartDialogClass)
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
NodeIDStartDialogClass::DoDataExchange (CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(NodeIDStartDialogClass)
	DDX_Control(pDX, IDC_ID_SPIN, m_IDSpin);
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(NodeIDStartDialogClass, CDialog)
	//{{AFX_MSG_MAP(NodeIDStartDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
NodeIDStartDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog();

	//
	//	Read the current value from the registry
	//
	int start_id = theApp.GetProfileInt (CONFIG_KEY, NODE_ID_START_VALUE, 100000);
	
	m_IDSpin.SetRange32 (100000, 1000000000);
	m_IDSpin.SetPos (start_id);
	SetDlgItemInt (IDC_ID, start_id);
		
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
NodeIDStartDialogClass::OnOK (void)
{
	//
	//	Store the new value in the registry
	//
	UINT start_id = GetDlgItemInt (IDC_ID);
	theApp.WriteProfileInt (CONFIG_KEY, NODE_ID_START_VALUE, start_id);

	//
	//	Update the node manager
	//
	NodeMgrClass::Reset_New_ID ();

	CDialog::OnOK ();
	return ;
}
