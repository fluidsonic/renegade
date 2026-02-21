#if !defined(AFX_EDITCONVERSATIONDIALOG_H__77BB0B01_5D9E_4BFF_9D25_CE964576950D__INCLUDED_)
#define AFX_EDITCONVERSATIONDIALOG_H__77BB0B01_5D9E_4BFF_9D25_CE964576950D__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
// Forward declarations
/////////////////////////////////////////////////////////////////////////////
class ConversationClass;
class ConversationRemarkClass;

/////////////////////////////////////////////////////////////////////////////
//
// EditConversationDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditConversationDialogClass : public CDialog
{
// Construction
public:
	EditConversationDialogClass (CWnd *pParent = NULL);
	~EditConversationDialogClass (void);

// Dialog Data
	//{{AFX_DATA(EditConversationDialogClass)
	enum { IDD = IDD_EDIT_CONVERSATION };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditConversationDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual BOOL OnCommand(WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditConversationDialogClass)
	afx_msg void OnDblclkRemarkList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnKeydownRemarkList(NMHDR* pNMHDR, LRESULT* pResult);
	virtual void OnOK();
	afx_msg void OnAdd();
	virtual BOOL OnInitDialog();
	afx_msg void OnInsert();
	afx_msg void OnItemchangedRemarkList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeleteitemRemarkList(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	/////////////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////////////
	void						Set_Conversation (ConversationClass *conversation);
	ConversationClass *	Peek_Conversation (void) { return m_Conversation; }

protected:

	/////////////////////////////////////////////////////////////////////////////
	//	Protected methods
	/////////////////////////////////////////////////////////////////////////////
	ConversationRemarkClass *Get_Entry_Data (int index);
	void						Add_Entry (const ConversationRemarkClass &remark, int insert_index = 0xFFFF);
	void						Update_Button_States (void);
	void						Update_Enable_State (int orator_index);
	void						Update_Player_Type_Combos (int orator_index);
	void						Update_Remarks (int orator_index);
	int						Get_Orator_Type (int orator_index);

	int						Find_Combobox_Entry (int orator_type);

	/////////////////////////////////////////////////////////////////////////////
	//	Protected member data
	/////////////////////////////////////////////////////////////////////////////
	ConversationClass *	m_Conversation;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITCONVERSATIONDIALOG_H__77BB0B01_5D9E_4BFF_9D25_CE964576950D__INCLUDED_)
