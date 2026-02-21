#if !defined(AFX_PRESETREMAPDIALOG_H__2412A947_7687_4FC9_AAC5_B3585396EB33__INCLUDED_)
#define AFX_PRESETREMAPDIALOG_H__2412A947_7687_4FC9_AAC5_B3585396EB33__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "vector.h"

/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class NodeClass;

/////////////////////////////////////////////////////////////////////////////
//
// PresetRemapDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class PresetRemapDialogClass : public CDialog
{
// Construction
public:
	PresetRemapDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(PresetRemapDialogClass)
	enum { IDD = IDD_PRESET_REMAP_DIALOG };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(PresetRemapDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(PresetRemapDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnDblclkListCtrl(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeleteitemListCtrl(NMHDR* pNMHDR, LRESULT* pResult);
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////////////
	//	Public Methods
	///////////////////////////////////////////////////////////////
	void	Set_Node_list (const DynamicVectorClass<NodeClass *> &node_list)	{ NodeList = node_list; }

private:

	///////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////
	DynamicVectorClass<NodeClass *>	NodeList;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PRESETREMAPDIALOG_H__2412A947_7687_4FC9_AAC5_B3585396EB33__INCLUDED_)
