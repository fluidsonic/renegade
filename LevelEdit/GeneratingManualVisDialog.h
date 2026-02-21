#if !defined(AFX_GENERATINGMANUALVISDIALOG_H__E2778314_7A17_49ED_98B2_2DDACB7F167F__INCLUDED_)
#define AFX_GENERATINGMANUALVISDIALOG_H__E2778314_7A17_49ED_98B2_2DDACB7F167F__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// GeneratingManualVisDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GeneratingManualVisDialogClass : public CDialog
{
// Construction
public:
	GeneratingManualVisDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GeneratingManualVisDialogClass)
	enum { IDD = IDD_GENERATING_MANUAL_VIS };
	CProgressCtrl	m_ProgressCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GeneratingManualVisDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GeneratingManualVisDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////////////////
	void			Set_Farm_Mode (int index, int total) { m_ProcessorIndex = index; m_TotalProcessors = total; m_FarmMode = true; }


private:

	///////////////////////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////////////////////
	int			Get_Manual_Point_Count (void);
	bool			On_Manual_Vis_Point_Render (DWORD milliseconds);
	void			Update_Time (void);

	///////////////////////////////////////////////////////////////////////////////
	//	Static methods
	///////////////////////////////////////////////////////////////////////////////
	static bool	ManualVisPointCallback (DWORD milliseconds, DWORD param);


	///////////////////////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////////////////////
	bool			m_Stop;
	bool			m_FarmMode;
	int			m_ProcessorIndex;
	int			m_TotalProcessors;
	int			m_TotalPoints;
	int			m_TotalTime;
	int			m_CurrentPoint;
	DWORD			m_StartTime;

	CString		m_StatusSection;
	CString		m_StatusFilename;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GENERATINGMANUALVISDIALOG_H__E2778314_7A17_49ED_98B2_2DDACB7F167F__INCLUDED_)
