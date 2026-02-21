#if !defined(AFX_GOTOOBJECTDIALOG_H__8558DB9C_B58D_11D2_9FF8_00104B791122__INCLUDED_)
#define AFX_GOTOOBJECTDIALOG_H__8558DB9C_B58D_11D2_9FF8_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// GotoObjectDialog.h : header file
//

// Forward declarations
class NodeClass;


/////////////////////////////////////////////////////////////////////////////
//
// GotoObjectDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GotoObjectDialogClass : public CDialog
{
// Construction
public:
	GotoObjectDialogClass (NodeClass *sel_node, CWnd *parent = NULL);

// Dialog Data
	//{{AFX_DATA(GotoObjectDialogClass)
	enum { IDD = IDD_GOTO_OBJECT };
	CComboBox	m_NodeList;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GotoObjectDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GotoObjectDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnEditChangeObjectList();
	afx_msg void OnSelChangeObjectList();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	
	////////////////////////////////////////////////////////////////
	//	Public Methods
	////////////////////////////////////////////////////////////////
	int				Get_Current_Selection (void);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	NodeClass *		m_pNode;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GOTOOBJECTDIALOG_H__8558DB9C_B58D_11D2_9FF8_00104B791122__INCLUDED_)
