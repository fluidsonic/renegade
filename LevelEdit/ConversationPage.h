#if !defined(AFX_CONVERSATIONPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_)
#define AFX_CONVERSATIONPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "dialogtoolbar.h"
#include "nodetypes.h"

/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class NodeClass;
class ConversationClass;


/////////////////////////////////////////////////////////////////////////////
//
// ConversationPageClass
//
/////////////////////////////////////////////////////////////////////////////
class ConversationPageClass : public CDialog
{
public:
	ConversationPageClass (CWnd *parent_wnd);
	virtual ~ConversationPageClass (void);

// Dialog Data
	//{{AFX_DATA(ConversationPageClass)
	enum { IDD = IDD_CONVERSATION_FORM };
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ConversationPageClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ConversationPageClass)
	afx_msg void OnDeleteItemConversationTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnSize(UINT nType, int cx, int cy);
	virtual BOOL OnInitDialog();
	afx_msg void OnDestroy();
	afx_msg void OnDblclkConversationTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnItemexpandedConversationTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnAdd();
	afx_msg void OnDelete();
	afx_msg void OnEdit();
	afx_msg void OnSwap();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void					Reload_Data (void);
	void					Reset_Tree (void);

	static ConversationPageClass *	Get_Instance (void)	{ return _TheInstance; }

protected:
	
	///////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////
	void					Insert_Entry (ConversationClass *conversation, bool sort_items = true);
	void					Edit_Entry (HTREEITEM tree_item);

	void					Reload_Tree (HTREEITEM *item_handle, ConversationClass **conversation_ptr);

private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	DialogToolbarClass	m_Toolbar;
	HTREEITEM				m_GlobalsRoot;
	HTREEITEM				m_LevelsRoot;
	
	static ConversationPageClass *	_TheInstance;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_CONVERSATIONPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_)
