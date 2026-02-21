#if !defined(AFX_GOTOGROUPDIALOG_H__C0BCECAC_B6EA_11D2_9FF8_00104B791122__INCLUDED_)
#define AFX_GOTOGROUPDIALOG_H__C0BCECAC_B6EA_11D2_9FF8_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// GotoGroupDialog.h : header file
//

// Forward declarations
class GroupMgrClass;

/////////////////////////////////////////////////////////////////////////////
//
// GotoGroupDialogClass dialog
//
class GotoGroupDialogClass : public CDialog
{
// Construction
public:
	GotoGroupDialogClass (GroupMgrClass *pgroup, CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GotoGroupDialogClass)
	enum { IDD = IDD_GOTO_GROUP };
	CComboBox	m_GroupList;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GotoGroupDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GotoGroupDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnEditChangeGroupList();
	afx_msg void OnSelChangeGroupList();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:
		
		////////////////////////////////////////////////////////////////
		//
		//	Public Methods
		//
		int						Get_Current_Selection (void);

	private:

		////////////////////////////////////////////////////////////////
		//
		//	Private member data
		//
		GroupMgrClass *		m_pGroup;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GOTOGROUPDIALOG_H__C0BCECAC_B6EA_11D2_9FF8_00104B791122__INCLUDED_)
