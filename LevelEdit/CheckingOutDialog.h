#if !defined(AFX_CHECKINGOUTDIALOG_H__2057225E_9559_11D2_9FED_00104B791122__INCLUDED_)
#define AFX_CHECKINGOUTDIALOG_H__2057225E_9559_11D2_9FED_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// CheckingOutDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CheckingOutDialogClass dialog

class CheckingOutDialogClass : public CDialog
{
// Construction
public:
	CheckingOutDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(CheckingOutDialogClass)
	enum { IDD = IDD_CHECKING_OUT };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CheckingOutDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	virtual BOOL OnCommand(WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CheckingOutDialogClass)
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_CHECKINGOUTDIALOG_H__2057225E_9559_11D2_9FED_00104B791122__INCLUDED_)
