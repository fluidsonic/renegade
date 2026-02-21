#if !defined(AFX_GOTOOBJECTBYIDDIALOG_H__70084A26_45E6_4AC2_81D5_1E258D56BA0D__INCLUDED_)
#define AFX_GOTOOBJECTBYIDDIALOG_H__70084A26_45E6_4AC2_81D5_1E258D56BA0D__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// GotoObjectByIDDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GotoObjectByIDDialogClass : public CDialog
{
// Construction
public:
	GotoObjectByIDDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GotoObjectByIDDialogClass)
	enum { IDD = IDD_GOTO_OBJECT_BY_ID };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GotoObjectByIDDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GotoObjectByIDDialogClass)
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GOTOOBJECTBYIDDIALOG_H__70084A26_45E6_4AC2_81D5_1E258D56BA0D__INCLUDED_)
