#if !defined(AFX_NODEIDSTARTDIALOG_H__D22A7607_3D84_11D4_A0A6_00104B791122__INCLUDED_)
#define AFX_NODEIDSTARTDIALOG_H__D22A7607_3D84_11D4_A0A6_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// NodeIDStartDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// NodeIDStartDialogClass dialog

class NodeIDStartDialogClass : public CDialog
{
// Construction
public:
	NodeIDStartDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(NodeIDStartDialogClass)
	enum { IDD = IDD_NODE_START_ID };
	CSpinButtonCtrl	m_IDSpin;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(NodeIDStartDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(NodeIDStartDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_NODEIDSTARTDIALOG_H__D22A7607_3D84_11D4_A0A6_00104B791122__INCLUDED_)
