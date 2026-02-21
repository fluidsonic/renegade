#if !defined(AFX_GENERATINGEDGESAMPLEDVISDIALOG_H__237F8914_2CCD_4BF4_B2E7_5BF5C975D44F__INCLUDED_)
#define AFX_GENERATINGEDGESAMPLEDVISDIALOG_H__237F8914_2CCD_4BF4_B2E7_5BF5C975D44F__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"

/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class VisGenProgressClass;

/////////////////////////////////////////////////////////////////////////////
//
// GeneratingEdgeSampledVisDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GeneratingEdgeSampledVisDialogClass : public CDialog
{
// Construction
public:
	GeneratingEdgeSampledVisDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GeneratingEdgeSampledVisDialogClass)
	enum { IDD = IDD_GENERATING_EDGE_SAMPLED_VIS };
	CProgressCtrl	m_ProgressCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GeneratingEdgeSampledVisDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GeneratingEdgeSampledVisDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnTimer(UINT nIDEvent);
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////////
	
	//
	//	Accessors
	//
	void							Set_Progress_Obj (VisGenProgressClass *obj);
	VisGenProgressClass *	Get_Progress_Obj (void) const	{ return m_ProgressObj; }

	void							Set_Finished (bool is_finished);
	void							Enable_Farm_Mode (bool onoff)	{ m_FarmMode = onoff; }

	//
	//	Static methods
	//
	static GeneratingEdgeSampledVisDialogClass *Display (void);

private:

	///////////////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////////////
	void							Update_Stats (void);
	void							End_Dialog (void);
	
	///////////////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////////////
	VisGenProgressClass *	m_ProgressObj;
	bool							m_IsCancelled;
	bool							m_IsFinished;
	DWORD							m_StartTime;

	bool							m_FarmMode;
	CString						m_StatusFilename;
	CString						m_StatusSection;

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GENERATINGEDGESAMPLEDVISDIALOG_H__237F8914_2CCD_4BF4_B2E7_5BF5C975D44F__INCLUDED_)
