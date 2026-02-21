// OptimizingVisDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "optimizingvisdialog.h"
#include "utils.h"
#include "sceneeditor.h"
#include "rendobj.h"
#include "phys.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


//////////////////////////////////////////////////////////////////////////
//	Local Prototypes
//////////////////////////////////////////////////////////////////////////
static UINT fnOptimizeVisDialogThread (DWORD dwparam1, DWORD dwparam2, DWORD, HRESULT *, HWND *);


/////////////////////////////////////////////////////////////////////////////
//
// OptimizingVisDialogClass
//
/////////////////////////////////////////////////////////////////////////////
OptimizingVisDialogClass::OptimizingVisDialogClass(CWnd *parent)
	:	m_Cancelled (false),
		m_ProgressStats (NULL),
		CDialog(OptimizingVisDialogClass::IDD, parent)
{
	//{{AFX_DATA_INIT(OptimizingVisDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT

	Create (OptimizingVisDialogClass::IDD, parent);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
OptimizingVisDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange (pDX);
	//{{AFX_DATA_MAP(OptimizingVisDialogClass)
	DDX_Control(pDX, IDC_PROGRESS_BAR, m_ProgressBar);
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(OptimizingVisDialogClass, CDialog)
	//{{AFX_MSG_MAP(OptimizingVisDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
OptimizingVisDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	m_ProgressBar.SetRange (0, 100);
	SetTimer (777, 1000, NULL);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnCancel
//
/////////////////////////////////////////////////////////////////////////////
void
OptimizingVisDialogClass::OnCancel (void)
{
	KillTimer (777);
	::DestroyWindow (m_hWnd);
	::PostQuitMessage (0);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// WindowProc
//
/////////////////////////////////////////////////////////////////////////////
LRESULT
OptimizingVisDialogClass::WindowProc
(
	UINT		message,
	WPARAM	wParam,
	LPARAM	lParam
)
{
	if (message == WM_TIMER) {
		Update_Stats ();
	}

	return CDialog::WindowProc (message, wParam, lParam);
}


/////////////////////////////////////////////////////////////////////////////
//
// Update_Stats
//
/////////////////////////////////////////////////////////////////////////////
void
OptimizingVisDialogClass::Update_Stats (void)
{
	if (m_ProgressStats == NULL) {
		return ;
	}

	//
	//	Update each of the status fields
	//
	SetDlgItemInt (IDC_INITIAL_BIT_COUNT_TEXT,				m_ProgressStats->Get_Initial_Bit_Count ());
	SetDlgItemInt (IDC_INITIAL_SECTOR_COUNT_TEXT,			m_ProgressStats->Get_Initial_Sector_Count ());
	SetDlgItemInt (IDC_FINAL_BIT_COUNT_TEXT,					m_ProgressStats->Get_Final_Bit_Count ());
	SetDlgItemInt (IDC_FINAL_SECTOR_COUNT_TEXT,				m_ProgressStats->Get_Final_Sector_Count ());
	SetDlgItemInt (IDC_INITIAL_OBJECT_COUNT_TEXT,			m_ProgressStats->Get_Initial_Object_Count ());
	SetDlgItemInt (IDC_FINAL_OBJECT_COUNT_TEXT,				m_ProgressStats->Get_Final_Object_Count ());
	SetDlgItemInt (IDC_INITIAL_DYNAMIC_CELL_COUNT_TEXT,	m_ProgressStats->Get_Initial_Dynamic_Cell_Count ());
	SetDlgItemInt (IDC_FINAL_DYNAMIC_CELL_COUNT_TEXT,		m_ProgressStats->Get_Final_Dynamic_Cell_Count ());
	SetDlgItemInt (IDC_DYNAMIC_CELLS_REMOVED_COUNT_TEXT,	m_ProgressStats->Get_Dynamic_Cells_Removed ());
	SetDlgItemInt (IDC_OBJECTS_MERGED_TEXT,					m_ProgressStats->Get_Objects_Merged ());
	SetDlgItemInt (IDC_SECTORS_MERGED_TEXT,					m_ProgressStats->Get_Sectors_Merged ());

	//
	//	Update the status text
	//
	int total	= m_ProgressStats->Get_Total_Operation_Count ();
	int current	= m_ProgressStats->Get_Completed_Operation_Count ();
	
	CString status_text;
	status_text.Format ("%d of %d operations completed.", current, total);
	SetDlgItemText (IDC_STATUS_TEXT,status_text);

	//
	//	Update the progress bar
	//
	if (total > 0) {
		m_ProgressBar.SetPos ((current * 100) / total);
	}

	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Set_Finished
//
/////////////////////////////////////////////////////////////////////////////
void
OptimizingVisDialogClass::Set_Finished (void)
{
	::EnableWindow (::GetDlgItem (m_hWnd, IDCANCEL), TRUE);
	Update_Stats ();
	m_ProgressBar.SetPos (100);
	
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Optimize
//
/////////////////////////////////////////////////////////////////////////////
void
OptimizingVisDialogClass::Optimize (void)
{
	OptimizingVisDialogClass *dialog = NULL;
	VisOptProgressClass stats;

	//
	//	Create the dialog on a separate thread
	//
	::Create_UI_Thread (fnOptimizeVisDialogThread, 0, (DWORD)&dialog, 0, NULL, NULL);
	dialog->Set_Status_Object (&stats);

	//
	//	Kick off the optimization
	//
	::Get_Scene_Editor ()->Optimize_Visibility_Data (stats);

	//
	//	Now make sure each dynamic object has updated visibility
	//	
	::Get_Scene_Editor ()->Reset_Dynamic_Object_Visibility_Status ();

	//
	//	Cleanup the dialog
	//
	dialog->Set_Finished ();
	dialog->Set_Status_Object (NULL);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// fnOptimizeVisDialogThread
//
////////////////////////////////////////////////////////////////////////////
UINT
fnOptimizeVisDialogThread
(
	DWORD dwparam1,
	DWORD dwparam2,
	DWORD /*dwparam3*/,
	HRESULT* /*presult*/,
	HWND* /*phmain_wnd*/
)
{
	OptimizingVisDialogClass *dialog = new OptimizingVisDialogClass (NULL); //::AfxGetMainWnd ());
	dialog->ShowWindow (SW_SHOW);

	//
	//	Return the dialog object to the caller
	//
	OptimizingVisDialogClass **return_val = (OptimizingVisDialogClass **)dwparam2;
	if (return_val != NULL) {
		(*return_val) = dialog;
	}

	return 1;
}

/////////////////////////////////////////////////////////////////////////////
//
// OptimizingVisDialogClass::Set_Status_Object 
//
////////////////////////////////////////////////////////////////////////////
void 
OptimizingVisDialogClass::Set_Status_Object (VisOptProgressClass *status_object)	
{ 
	m_ProgressStats = status_object; 
	Update_Stats();
	
	return ;
}
