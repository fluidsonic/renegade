#if !defined(AFX_NEWHEIGHTFIELDDIALOG_H__640FCB1B_F23F_4213_B597_6D63E7EEB371__INCLUDED_)
#define AFX_NEWHEIGHTFIELDDIALOG_H__640FCB1B_F23F_4213_B597_6D63E7EEB371__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// NewHeightfieldDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// NewHeightfieldDialogClass dialog

class NewHeightfieldDialogClass : public CDialog
{
// Construction
public:
	NewHeightfieldDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(NewHeightfieldDialogClass)
	enum { IDD = IDD_HEIGHTFIELD_CREATE };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(NewHeightfieldDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(NewHeightfieldDialogClass)
	virtual void OnOK();
	virtual BOOL OnInitDialog();
	afx_msg void OnBrowse();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_NEWHEIGHTFIELDDIALOG_H__640FCB1B_F23F_4213_B597_6D63E7EEB371__INCLUDED_)
