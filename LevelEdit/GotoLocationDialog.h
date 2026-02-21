#if !defined(AFX_GOTOLOCATIONDIALOG_H__8624772D_22C1_11D4_A09F_00104B791122__INCLUDED_)
#define AFX_GOTOLOCATIONDIALOG_H__8624772D_22C1_11D4_A09F_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// GotoLocationDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GotoLocationDialogClass : public CDialog
{
// Construction
public:
	GotoLocationDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GotoLocationDialogClass)
	enum { IDD = IDD_GOTO_LOCATION };
	CSpinButtonCtrl	m_ZPosSpin;
	CSpinButtonCtrl	m_YPosSpin;
	CSpinButtonCtrl	m_XPosSpin;
	CSpinButtonCtrl	m_FacingSpin;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GotoLocationDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GotoLocationDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GOTOLOCATIONDIALOG_H__8624772D_22C1_11D4_A09F_00104B791122__INCLUDED_)
