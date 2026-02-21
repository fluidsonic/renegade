#if !defined(AFX_OPTIMIZINGISDIALOG_H__4F3E7431_C94F_11D3_A085_00104B791122__INCLUDED_)
#define AFX_OPTIMIZINGVISDIALOG_H__4F3E7431_C94F_11D3_A085_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "visoptprogress.h"


/////////////////////////////////////////////////////////////////////////////
//
// OptimizingVisDialogClass dialog
//
/////////////////////////////////////////////////////////////////////////////
class OptimizingVisDialogClass : public CDialog
{
// Construction
public:
	OptimizingVisDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(OptimizingVisDialogClass)
	enum { IDD = IDD_OPTIMIZING_VIS };
	CProgressCtrl	m_ProgressBar;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(OptimizingVisDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(OptimizingVisDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	/////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////
	bool			Was_Cancelled (void) const											{ return m_Cancelled; }
	void			Set_Status_Object (VisOptProgressClass *status_object);
	void			Set_Finished (void);

	/////////////////////////////////////////////////////////////
	//	Static methods
	/////////////////////////////////////////////////////////////
	static void	Optimize (void);

private:

	/////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////
	void			Update_Stats (void);

	/////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////
	bool							m_Cancelled;
	VisOptProgressClass	*	m_ProgressStats;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_OPTIMIZINGVISDIALOG_H__4F3E7431_C94F_11D3_A085_00104B791122__INCLUDED_)
