#if !defined(AFX_OVERLAPPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_)
#define AFX_OVERLAPPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_

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


/////////////////////////////////////////////////////////////////////////////
//
// OverlapPageClass dialog
//
/////////////////////////////////////////////////////////////////////////////
class OverlapPageClass : public CDialog
{
public:
	OverlapPageClass (CWnd *parent_wnd);
	virtual ~OverlapPageClass (void);

// Dialog Data
	//{{AFX_DATA(OverlapPageClass)
	enum { IDD = IDD_OVERLAP_FORM };
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(OverlapPageClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(OverlapPageClass)
	afx_msg void OnDeleteitemOverlapTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnSize(UINT nType, int cx, int cy);
	virtual BOOL OnInitDialog();
	afx_msg void OnDestroy();
	afx_msg void OnRefresh();
	afx_msg void OnDblclkOverlapTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnItemexpandedOverlapTree(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void					Build_Tree (void);
	void					Reset_Tree (void);

protected:
	
	///////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////
	void					Detect_Overlaps (HTREEITEM root_item, NODE_TYPE node_type);
	HTREEITEM			Insert_Node (HTREEITEM parent_item, NodeClass *node);

private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	DialogToolbarClass	m_Toolbar;
	HTREEITEM				m_SoundsRoot;
	HTREEITEM				m_LightsRoot;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_OVERLAPPAGE_H__69EA03B9_0F32_11D4_A098_00104B791122__INCLUDED_)
