#if !defined(AFX_UPDATINGDBDIALOG_H__A5E14BDC_8F04_11D2_9FE7_00104B791122__INCLUDED_)
#define AFX_UPDATINGDBDIALOG_H__A5E14BDC_8F04_11D2_9FE7_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// UpdatingDBDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// UpdatingDBDialogClass dialog

class UpdatingDBDialogClass : public CDialog
{
// Construction
public:
	UpdatingDBDialogClass(HWND hparentwnd);   // standard constructor

// Dialog Data
	//{{AFX_DATA(UpdatingDBDialogClass)
	enum { IDD = IDD_UPDATING_DATABASE };
	CAnimateCtrl	m_AnimationCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(UpdatingDBDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	virtual void PostNcDestroy();
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(UpdatingDBDialogClass)
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_UPDATINGDBDIALOG_H__A5E14BDC_8F04_11D2_9FE7_00104B791122__INCLUDED_)
