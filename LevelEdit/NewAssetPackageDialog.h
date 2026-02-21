#if !defined(AFX_NEWASSETPACKAGEDIALOG_H__C37F6692_87EF_4A1C_AAFE_AE04BA2B2C24__INCLUDED_)
#define AFX_NEWASSETPACKAGEDIALOG_H__C37F6692_87EF_4A1C_AAFE_AE04BA2B2C24__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"


/////////////////////////////////////////////////////////////////////////////
//
// NewAssetPackageDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class NewAssetPackageDialogClass : public CDialog
{
// Construction
public:
	NewAssetPackageDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(NewAssetPackageDialogClass)
	enum { IDD = IDD_NEW_PACKAGE_NAME };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(NewAssetPackageDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(NewAssetPackageDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	
	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////
	void					Set_Package_Name (const char *name)	{ PackageName = name; }
	const CString &	Get_Package_Name (void)	const			{ return PackageName; }

protected:
	
	//////////////////////////////////////////////////////////////
	//	Protected methods
	//////////////////////////////////////////////////////////////
	bool					Is_Duplicate_Name (const CString &name);
	bool					Is_Valid_Name (const CString &name);

private:

	//////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////
	CString			PackageName;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_NEWASSETPACKAGEDIALOG_H__C37F6692_87EF_4A1C_AAFE_AE04BA2B2C24__INCLUDED_)
