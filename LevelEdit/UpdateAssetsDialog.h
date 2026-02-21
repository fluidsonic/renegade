#if !defined(AFX_UPDATEASSETSDIALOG_H__E2337FFF_C5CF_11D2_9FFA_00104B791122__INCLUDED_)
#define AFX_UPDATEASSETSDIALOG_H__E2337FFF_C5CF_11D2_9FFA_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "listtypes.h"


/////////////////////////////////////////////////////////////////////////////
//
// UpdateAssetsDialogClass dialog
//
/////////////////////////////////////////////////////////////////////////////
class UpdateAssetsDialogClass : public CDialog
{
// Construction
public:
	UpdateAssetsDialogClass (const CString &comments, STRING_LIST &directory_list, bool update_all, CWnd *pParent = NULL);

// Dialog Data
	//{{AFX_DATA(UpdateAssetsDialogClass)
	enum { IDD = IDD_PERFORM_UPDATE };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(UpdateAssetsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(UpdateAssetsDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:
		
		////////////////////////////////////////////////////////
		//	Public methods
		////////////////////////////////////////////////////////
		
	private:

		////////////////////////////////////////////////////////
		//	Private member data
		////////////////////////////////////////////////////////
		CString					m_Comments;
		STRING_LIST &			m_DirectoryList;
		bool						m_UpdateAll;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_UPDATEASSETSDIALOG_H__E2337FFF_C5CF_11D2_9FFA_00104B791122__INCLUDED_)
