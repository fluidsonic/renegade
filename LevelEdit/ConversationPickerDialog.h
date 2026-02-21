#if !defined(AFX_CONVERSATIONPICKERDIALOG_H__45E7AEA7_6692_43BF_B169_555904929404__INCLUDED_)
#define AFX_CONVERSATIONPICKERDIALOG_H__45E7AEA7_6692_43BF_B169_555904929404__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"

/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class ConversationClass;


/////////////////////////////////////////////////////////////////////////////
//
// ConversationPickerDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class ConversationPickerDialogClass : public CDialog
{
// Construction
public:
	ConversationPickerDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(ConversationPickerDialogClass)
	enum { IDD = IDD_CONVERSATION_PICKER };
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ConversationPickerDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ConversationPickerDialogClass)
	afx_msg void OnSelchangedConversationTree(NMHDR* pNMHDR, LRESULT* pResult);
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	//////////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////////
	void						Set_Conversation (ConversationClass *conversation)	{ Conversation = conversation; }
	ConversationClass	*	Get_Conversation (void)										{ return Conversation; }

private:

	//////////////////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////////////////
	void						Insert_Entry (ConversationClass *conversation);

	//////////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////////
	ConversationClass *	Conversation;
	HTREEITEM				GlobalsRoot;
	HTREEITEM				LevelsRoot;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_CONVERSATIONPICKERDIALOG_H__45E7AEA7_6692_43BF_B169_555904929404__INCLUDED_)
