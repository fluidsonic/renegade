#if !defined(AFX_STRINGSCATEGORYNAMEDIALOG_H__ADCB3001_73C2_42EE_BA38_6DAFF76AFF85__INCLUDED_)
#define AFX_STRINGSCATEGORYNAMEDIALOG_H__ADCB3001_73C2_42EE_BA38_6DAFF76AFF85__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"

/////////////////////////////////////////////////////////////////////////////
//
// StringsCategoryNameDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class StringsCategoryNameDialogClass : public CDialog
{
// Construction
public:
	StringsCategoryNameDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(StringsCategoryNameDialogClass)
	enum { IDD = IDD_STRING_CATEGORY };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(StringsCategoryNameDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(StringsCategoryNameDialogClass)
	virtual void OnOK();
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()


public:

	/////////////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////////////
	void				Set_Name (const char *name)	{ Name = name; }
	const char *	Get_Name (void) const			{ return Name; }

private:

	/////////////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////////////
	CString		Name;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_STRINGSCATEGORYNAMEDIALOG_H__ADCB3001_73C2_42EE_BA38_6DAFF76AFF85__INCLUDED_)
