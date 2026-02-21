#if !defined(AFX_EXPORTPRESETSDIALOG_H__6A8D6C2E_8041_4FC3_AF0D_6C418730CE62__INCLUDED_)
#define AFX_EXPORTPRESETSDIALOG_H__6A8D6C2E_8041_4FC3_AF0D_6C418730CE62__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// ExportPresetsDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class ExportPresetsDialogClass : public CDialog
{
// Construction
public:
	ExportPresetsDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(ExportPresetsDialogClass)
	enum { IDD = IDD_EXPORT_PRESET_SETTINGS };
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ExportPresetsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ExportPresetsDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnBrowse();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////

private:

	///////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////
	void			Fill_Tree (void);
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EXPORTPRESETSDIALOG_H__6A8D6C2E_8041_4FC3_AF0D_6C418730CE62__INCLUDED_)
