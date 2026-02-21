// LightSolveProgressDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "LightSolveProgressDialog.h"
#include "utils.h"
#include "sceneeditor.h"
#include "lightsolvecontext.h"
#include "lightsolveprogress.h"
#include "phys.h"
#include "rendobj.h"
#include "lightsolve.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


//////////////////////////////////////////////////////////////////////////
//	Progress Callback Object
//////////////////////////////////////////////////////////////////////////
class LSProgressCallbackClass : public LightSolveObserverClass
{	
public:
	LSProgressCallbackClass(void) { }
	~LSProgressCallbackClass(void) { }

	virtual void Progress_Callback(LightSolveContextClass & context) 
	{ 
		General_Pump_Messages(); 
	};
};



/////////////////////////////////////////////////////////////////////////////
// LightSolveProgressDialog dialog


LightSolveProgressDialog::LightSolveProgressDialog(LightSolveContextClass & context,CWnd* pParent /*=NULL*/)
	:	m_Cancelled(false),
		m_SolveContext(context),
		CDialog(LightSolveProgressDialog::IDD, pParent)
{
	//{{AFX_DATA_INIT(LightSolveProgressDialog)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


void LightSolveProgressDialog::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(LightSolveProgressDialog)
	DDX_Control(pDX, IDC_PROGRESS_BAR, m_ProgressBar);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(LightSolveProgressDialog, CDialog)
	//{{AFX_MSG_MAP(LightSolveProgressDialog)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// LightSolveProgressDialog message handlers


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL 
LightSolveProgressDialog::OnInitDialog() 
{
	CDialog::OnInitDialog();
	
	m_ProgressBar.SetRange (0, 100);
	SetTimer (777, 250, NULL);
	ShowWindow (SW_SHOW);

	PostMessage (WM_USER+101);
	return TRUE;
}

/////////////////////////////////////////////////////////////////////////////
//
// OnCancel
//
/////////////////////////////////////////////////////////////////////////////
void
LightSolveProgressDialog::OnCancel (void)
{
	m_Cancelled = true;
	m_SolveContext.Get_Progress().Request_Cancel();

	return ;
}

/////////////////////////////////////////////////////////////////////////////
//
// WindowProc
//
/////////////////////////////////////////////////////////////////////////////
LRESULT 
LightSolveProgressDialog::WindowProc(UINT message, WPARAM wParam, LPARAM lParam) 
{
	if (message == WM_USER+101) {
		//
		//	Kick off the lighting solveoptimization
		//
		LightSolveClass::Generate_Static_Light_Solve(m_SolveContext); 

		//
		//	Cleanup the dialog
		//
		Set_Finished ();
	}

	if (message == WM_TIMER) {
		Update_Stats ();
	}
	
	return CDialog::WindowProc(message, wParam, lParam);
}

/////////////////////////////////////////////////////////////////////////////
//
// Set_Finished
//
/////////////////////////////////////////////////////////////////////////////
void
LightSolveProgressDialog::Set_Finished (void)
{
	Update_Stats ();
	m_ProgressBar.SetPos (100);
	EndDialog (IDOK);
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// Update_Stats
//
/////////////////////////////////////////////////////////////////////////////
void
LightSolveProgressDialog::Update_Stats (void)
{
	::General_Pump_Messages();

	LightSolveProgressClass * progress = &(m_SolveContext.Get_Progress());

	//
	//	Update each of the status fields
	//
	SetDlgItemInt (IDC_TOTAL_OBJECT_COUNT,		progress->Get_Object_Count());
	SetDlgItemInt (IDC_CURRENT_OBJECT,			progress->Get_Processed_Object_Count());
	SetDlgItemText (IDC_CURRENT_MESH_NAME,		progress->Get_Current_Mesh_Name());
	SetDlgItemInt (IDC_VERTEX_COUNT,				progress->Get_Current_Mesh_Vertex_Count());
	SetDlgItemInt (IDC_CURRENT_VERTEX,			progress->Get_Current_Vertex());
	
	//
	//	Update the status text
	//
	int total	= progress->Get_Object_Count ();
	int current	= progress->Get_Processed_Object_Count ();
	
	CString status_text;
	status_text.Format ("%d of %d objects solved.", current, total);
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
// Solve
//
////////////////////////////////////////////////////////////////////////////
void
LightSolveProgressDialog::Solve (LightSolveContextClass & context,CWnd * parent)
{
	//
	// Create the callback object
	LSProgressCallbackClass	callback;
	context.Set_Observer(&callback);

	//
	//	Create the dialog
	//
	LightSolveProgressDialog dialog(context,parent);
	dialog.DoModal();
}

	
