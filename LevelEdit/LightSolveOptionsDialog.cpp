// LightSolveOptionsDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "sceneeditor.h"
#include "utils.h"
#include "lightsolveoptionsdialog.h"
#include "lightsolveprogressdialog.h"
#include "phys.h"
#include "rendobj.h"
#include "lightsolvecontext.h"
#include "pscene.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// LightSolveOptionsDialogClass dialog


LightSolveOptionsDialogClass::LightSolveOptionsDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(LightSolveOptionsDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(LightSolveOptionsDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
}


void LightSolveOptionsDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(LightSolveOptionsDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(LightSolveOptionsDialogClass, CDialog)
	//{{AFX_MSG_MAP(LightSolveOptionsDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// LightSolveOptionsDialogClass message handlers

BOOL LightSolveOptionsDialogClass::OnInitDialog() 
{
	CDialog::OnInitDialog();
	return TRUE;
}

void LightSolveOptionsDialogClass::OnOK() 
{
	LightSolveContextClass context;
	SceneEditorClass *scene = ::Get_Scene_Editor ();

	//
	// Setup the solve options
	//
	context.Enable_Occlusion(SendDlgItemMessage(IDC_LIGHT_SOLVE_OCCLUSION,BM_GETCHECK));
	context.Enable_Filtering(SendDlgItemMessage(IDC_LIGHT_SOLVE_FILTERING,BM_GETCHECK));

	//
	// Run the solve
	//
	LightSolveProgressDialog::Solve(context,this);
	
	CDialog::OnOK ();	
	return ;
}

