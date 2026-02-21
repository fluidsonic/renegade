#if !defined(AFX_VISSTATSDIALOG_H__0F5680CF_3805_11D4_A0A2_00104B791122__INCLUDED_)
#define AFX_VISSTATSDIALOG_H__0F5680CF_3805_11D4_A0A2_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// VisStatsDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class VisStatsDialogClass : public CDialog
{
// Construction
public:
	VisStatsDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(VisStatsDialogClass)
	enum { IDD = IDD_VIS_STATS };
	CListCtrl	m_SectorList;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(VisStatsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(VisStatsDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnDblclkSectorList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDeleteitemSectorList(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnColumnclickSectorList(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

private:

	////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////
	int			m_CurrentColSort;
	bool			m_AscendingSort;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_VISSTATSDIALOG_H__0F5680CF_3805_11D4_A0A2_00104B791122__INCLUDED_)
