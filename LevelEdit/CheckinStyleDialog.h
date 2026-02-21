#if !defined(AFX_CHECKINSTYLEDIALOG_H__2057225F_9559_11D2_9FED_00104B791122__INCLUDED_)
#define AFX_CHECKINSTYLEDIALOG_H__2057225F_9559_11D2_9FED_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// CheckinStyleDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CheckinStyleDialogClass dialog

class CheckinStyleDialogClass : public CDialog
{
// Construction
public:
	CheckinStyleDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(CheckinStyleDialogClass)
	enum { IDD = IDD_CHECKIN_STYLE };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CheckinStyleDialogClass)
	public:
	virtual int DoModal();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual BOOL OnCommand(WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CheckinStyleDialogClass)
	virtual void OnOK();
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_CHECKINSTYLEDIALOG_H__2057225F_9559_11D2_9FED_00104B791122__INCLUDED_)
