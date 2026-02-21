// GenerateVisDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "GenerateVisDialog.h"
#include "GeneratingVisDialog.H"
#include "Utils.H"
#include "SceneEditor.H"
#include "rendobj.h"
#include "phys.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// GenerateVisDialogClass dialog


GenerateVisDialogClass::GenerateVisDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(GenerateVisDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(GenerateVisDialogClass)
	//}}AFX_DATA_INIT
}


void GenerateVisDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(GenerateVisDialogClass)
	DDX_Control(pDX, IDC_SAMPLEHEIGHT_SLIDER, m_SampleHeightSlider);
	DDX_Control(pDX, IDC_GRANULARITY_SLIDER, m_GranularitySlider);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(GenerateVisDialogClass, CDialog)
	//{{AFX_MSG_MAP(GenerateVisDialogClass)
	ON_WM_HSCROLL()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


//////////////////////////////////////////////////////////////////////////////
//
//	OnInitDialog
//
//////////////////////////////////////////////////////////////////////////////
BOOL
GenerateVisDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	m_GranularitySlider.SetRange (1, 32);
	m_GranularitySlider.SetPos (16);	
	SetDlgItemInt (IDC_GRANULARITY_EDIT, 16);

	m_SampleHeightSlider.SetRange (1, 20);
	m_SampleHeightSlider.SetPos (10);
	SetDlgItemInt (IDC_SAMPLEHEIGHT_EDIT, 10);

	bool bcheck = ::Get_Scene_Editor ()->Is_Vis_Quick_And_Dirty();
	SendDlgItemMessage (IDC_IGNORE_TRANSPARENCY_CHECK, BM_SETCHECK, (WPARAM)bcheck);
	return TRUE;
}


//////////////////////////////////////////////////////////////////////////////
//
//	OnHScroll
//
//////////////////////////////////////////////////////////////////////////////
void
GenerateVisDialogClass::OnHScroll
(
	UINT nSBCode,
	UINT nPos,
	CScrollBar* pScrollBar
) 
{
	SetDlgItemInt (IDC_GRANULARITY_EDIT, m_GranularitySlider.GetPos ());
	SetDlgItemInt (IDC_SAMPLEHEIGHT_EDIT, m_SampleHeightSlider.GetPos ());
	CDialog::OnHScroll(nSBCode, nPos, pScrollBar);
	return ;
}


//////////////////////////////////////////////////////////////////////////////
//
//	OnOK
//
//////////////////////////////////////////////////////////////////////////////
void
GenerateVisDialogClass::OnOK (void) 
{
	SceneEditorClass *scene = ::Get_Scene_Editor ();

	//
	//	Enable quick and dirty mode (if necessary)
	//
	BOOL is_quick_and_dirty = SendDlgItemMessage (IDC_IGNORE_TRANSPARENCY_CHECK, BM_GETCHECK);
	scene->Set_Vis_Quick_And_Dirty (is_quick_and_dirty == TRUE);	

	//
	//	Read the settings from the dialog
	//
	BOOL ignore_bias		= SendDlgItemMessage (IDC_IGNORE_VIS_BIAS, BM_GETCHECK);
	BOOL selection_only	= SendDlgItemMessage (IDC_SELECTION_ONLY, BM_GETCHECK);
	float granularity		= (float)m_GranularitySlider.GetPos ();
	float sample_height	= (float)m_SampleHeightSlider.GetPos ();

	// 
	// Record the time when the vis preprocessing is started
	//
	DWORD start_time = ::GetTickCount();

	//
	// Reset the vis data (which also causes the culling systems to re-partition)
	//
	if (selection_only != TRUE) {
		scene->Reset_Vis(true);
	}
	
	//
	//	Kick off the vis
	//
	if (SendDlgItemMessage (IDC_USE_EDGE_SAMPLING, BM_GETCHECK) != 0) {
		scene->Generate_Edge_Sampled_Vis (granularity, (ignore_bias == TRUE));
	} else {
		scene->Generate_Uniform_Sampled_Vis (granularity, sample_height, (ignore_bias == TRUE), (selection_only == TRUE));
	}

	if (selection_only != TRUE) {
		
		// 
		// Now do the manual vis points ();
		//
		scene->Generate_Manual_Vis ();

		// 
		// Now vis the light sources
		//
		scene->Generate_Light_Vis ();
	}

	//
	//	Now make sure each dynamic object has updated visibility
	//
	scene->Reset_Dynamic_Object_Visibility_Status ();

	//
	// Display the total elapsed time to the user
	//
	DWORD elapsed_time = ::GetTickCount() - start_time;
	int hours = elapsed_time / (1000 * 60 * 60);
	elapsed_time -= hours * (1000 * 60 * 60);
	int minutes = elapsed_time / (1000 * 60);
	elapsed_time -= minutes * (1000 * 60);
	int seconds = elapsed_time / 1000;
	
	CString message;
	message.Format("Total Elapsed Time: %d hours, %d minutes, %d seconds.",hours,minutes,seconds);
	MessageBox (message, "Time", MB_OK | MB_ICONEXCLAMATION);


	CDialog::OnOK ();	
	return ;
}
