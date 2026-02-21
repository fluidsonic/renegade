#if !defined(AFX_MEMLOGDIALOG_H__3F520D5C_1115_4B53_9FAE_EEDE084ADD9B__INCLUDED_)
#define AFX_MEMLOGDIALOG_H__3F520D5C_1115_4B53_9FAE_EEDE084ADD9B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// MemLogDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// MemLogDialogClass dialog

class MemLogDialogClass : public CDialog
{
// Construction
public:
	MemLogDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(MemLogDialogClass)
	enum { IDD = IDD_MEMLOG };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(MemLogDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(MemLogDialogClass)
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_MEMLOGDIALOG_H__3F520D5C_1115_4B53_9FAE_EEDE084ADD9B__INCLUDED_)
