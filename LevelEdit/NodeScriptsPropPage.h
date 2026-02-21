#if !defined(AFX_OBJECTSCRIPTSPROPPAGE_H__1A8EC038_8874_11D2_9FE1_00104B791122__INCLUDED_)
#define AFX_OBJECTSCRIPTSPROPPAGE_H__1A8EC038_8874_11D2_9FE1_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// NodeScriptsPropPage.h : header file
//

#include "dockableform.h"
#include "listtypes.h"
#include "resource.h"


//////////////////////////////////////////////////////////////////////////////
// Forward declarations
//////////////////////////////////////////////////////////////////////////////
class EditScriptClass;
class ScriptListParameterClass;


//////////////////////////////////////////////////////////////////////////////
//
//	NodeScriptsPropPage
//
//////////////////////////////////////////////////////////////////////////////
class NodeScriptsPropPage : public DockableFormClass 
{
	public:
		NodeScriptsPropPage (void);
		NodeScriptsPropPage (SCRIPT_LIST *script_list);
		virtual ~NodeScriptsPropPage (void);

// Form Data
public:
	//{{AFX_DATA(NodeScriptsPropPage)
	enum { IDD = IDD_OBJECT_SCRIPTS };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(NodeScriptsPropPage)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:	
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(NodeScriptsPropPage)
	afx_msg void OnItemChangedScriptList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnAdd();
	afx_msg void OnDelete();
	afx_msg void OnModify();
	afx_msg void OnDeleteItemScriptList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDblclkScriptlist(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	
public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void						HandleInitDialog (void);
	virtual bool			Apply_Changes (void);

	void						Set_Script_List_Parameter (ScriptListParameterClass *param)	{ m_ScriptListParam = param; }

protected:

	///////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////
	void						Build_Script_List (void);

private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	SCRIPT_LIST *						m_ScriptList;
	ScriptListParameterClass *		m_ScriptListParam;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_OBJECTSCRIPTSPROPPAGE_H__1A8EC038_8874_11D2_9FE1_00104B791122__INCLUDED_)
