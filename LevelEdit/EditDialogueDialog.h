#if !defined(AFX_EDITDIALOGUEDIALOG_H__BC974F00_1204_4644_96D7_7BFC1BC5BBCC__INCLUDED_)
#define AFX_EDITDIALOGUEDIALOG_H__BC974F00_1204_4644_96D7_7BFC1BC5BBCC__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class DialogueClass;
class DialogueOptionClass;


/////////////////////////////////////////////////////////////////////////////
//
// EditDialogueDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditDialogueDialogClass : public CDialog
{
public:
	EditDialogueDialogClass (CWnd *pParent = NULL);

// Dialog Data
	//{{AFX_DATA(EditDialogueDialogClass)
	enum { IDD = IDD_EDIT_DIALOGUE };
	CSpinButtonCtrl	m_SelectedWeightSpin;
	CSpinButtonCtrl	m_SilenceWeightSpin;
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditDialogueDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditDialogueDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnAdd();
	afx_msg void OnDelete();
	afx_msg void OnDblclkOptionList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeleteitemOptionList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnItemchangedRemarkList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeltaposSelectedWeightSpin(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnKillfocusSelectedWeightEdit();
	afx_msg void OnVScroll(UINT nSBCode, UINT nPos, CScrollBar* pScrollBar);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	void					Set_Dialogue (DialogueClass *dialogue)	{ m_Dialogue = dialogue; }
	DialogueClass *	Get_Dialogue (void) const					{ return m_Dialogue; }


protected:

	////////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////////
	void					Update_Entry (int index);	
	void					Insert_Entry (DialogueOptionClass *option);

	void					Update_Enabled_State (void);
	void					Update_Current_Weight (void);

private:

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	DialogueClass *	m_Dialogue;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITDIALOGUEDIALOG_H__BC974F00_1204_4644_96D7_7BFC1BC5BBCC__INCLUDED_)
