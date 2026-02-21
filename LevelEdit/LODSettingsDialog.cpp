// LODSettingsDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "LODSettingsDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
//
// LODSettingsDialogClass
//
/////////////////////////////////////////////////////////////////////////////
LODSettingsDialogClass::LODSettingsDialogClass (unsigned int distance, CWnd *pParent)
	:	m_Distance (distance),
		CDialog(LODSettingsDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(LODSettingsDialogClass)
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
LODSettingsDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(LODSettingsDialogClass)
	DDX_Control(pDX, IDC_DISTANCE_SPIN, m_DistanceSpin);
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(LODSettingsDialogClass, CDialog)
	//{{AFX_MSG_MAP(LODSettingsDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
LODSettingsDialogClass::OnInitDialog (void) 
{
	CDialog::OnInitDialog ();

	//
	//	Setup the distance controls
	//
	m_DistanceSpin.SetRange (0, 10000);
	m_DistanceSpin.SetPos (m_Distance);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
LODSettingsDialogClass::OnOK (void) 
{
	m_Distance = m_DistanceSpin.GetPos ();
	CDialog::OnOK ();
	return ;
}

