#if !defined(AFX_EDITCONVERSATIONLISTDIALOG_H__29A296FF_374F_46F9_A2F8_496C5307D103__INCLUDED_)
#define AFX_EDITCONVERSATIONLISTDIALOG_H__29A296FF_374F_46F9_A2F8_496C5307D103__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//	 Forward declarations
/////////////////////////////////////////////////////////////////////////////
class ConversationClass;

/////////////////////////////////////////////////////////////////////////////
//
// EditConversationListDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditConversationListDialogClass : public CDialog
{
// Construction
public:
	EditConversationListDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(EditConversationListDialogClass)
	enum { IDD = IDD_CONVERSATION_LIST };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditConversationListDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditConversationListDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnAdd();
	virtual void OnOK();
	afx_msg void OnDblclkConversationList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeleteitemConversationList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnKeydownConversationList(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	/////////////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////////////

protected:

	/////////////////////////////////////////////////////////////////////////////
	//	Protected methods
	/////////////////////////////////////////////////////////////////////////////
	void				Add_Conversation (ConversationClass *conversation);
	void				Update_Conversation (int sel_index);
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITCONVERSATIONLISTDIALOG_H__29A296FF_374F_46F9_A2F8_496C5307D103__INCLUDED_)
