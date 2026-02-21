// GenerateBaseDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "GenerateBaseDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// GenerateBaseDialogClass dialog


GenerateBaseDialogClass::GenerateBaseDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(GenerateBaseDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(GenerateBaseDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}


void GenerateBaseDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(GenerateBaseDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(GenerateBaseDialogClass, CDialog)
	//{{AFX_MSG_MAP(GenerateBaseDialogClass)
	ON_EN_UPDATE(IDC_PRESET_NAME, OnUpdatePresetName)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// GenerateBaseDialogClass message handlers

void
GenerateBaseDialogClass::OnOK (void) 
{
	GetDlgItemText (IDC_PRESET_NAME, m_PresetName);
	CDialog::OnOK();
	return ;
}

void
GenerateBaseDialogClass::OnUpdatePresetName (void)
{
	// Enable/disable the OK button based on the entry in the name editfield
	BOOL benable = (::GetWindowTextLength (::GetDlgItem (m_hWnd, IDC_PRESET_NAME)) > 0);
	::EnableWindow (::GetDlgItem (m_hWnd, IDOK), benable);
	return ;
}
