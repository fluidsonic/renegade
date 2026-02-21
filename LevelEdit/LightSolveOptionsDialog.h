#if !defined(AFX_LIGHTSOLVEOPTIONSDIALOG_H__71E8C194_1511_4569_8F11_D27772ED5159__INCLUDED_)
#define AFX_LIGHTSOLVEOPTIONSDIALOG_H__71E8C194_1511_4569_8F11_D27772ED5159__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// LightSolveOptionsDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// LightSolveOptionsDialogClass dialog

class LightSolveOptionsDialogClass : public CDialog
{
// Construction
public:
	LightSolveOptionsDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(LightSolveOptionsDialogClass)
	enum { IDD = IDD_LIGHT_SOLVE_OPTIONS };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(LightSolveOptionsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(LightSolveOptionsDialogClass)
	virtual void OnOK();
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_LIGHTSOLVEOPTIONSDIALOG_H__71E8C194_1511_4569_8F11_D27772ED5159__INCLUDED_)
