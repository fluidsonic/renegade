#if !defined(AFX_EDITFILENAMELISTDIALOG_H__C6B399CD_EACA_4457_9796_05B81F576E82__INCLUDED_)
#define AFX_EDITFILENAMELISTDIALOG_H__C6B399CD_EACA_4457_9796_05B81F576E82__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "vector.h"
#include "wwstring.h"


/////////////////////////////////////////////////////////////////////////////
//
// EditFilenameListDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditFilenameListDialogClass : public CDialog
{
// Construction
public:
	EditFilenameListDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(EditFilenameListDialogClass)
	enum { IDD = IDD_EDIT_FILENAME_LIST };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditFilenameListDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditFilenameListDialogClass)
	afx_msg void OnAdd();
	afx_msg void OnKeydownFilenameListCtrl(NMHDR* pNMHDR, LRESULT* pResult);
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	////////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////////
	DynamicVectorClass<StringClass> &	Get_List (void) { return m_List; }
	void											Set_List (DynamicVectorClass<StringClass> &list) { m_List = list; }

private:

	////////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////////
	DynamicVectorClass<StringClass>	m_List;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITFILENAMELISTDIALOG_H__C6B399CD_EACA_4457_9796_05B81F576E82__INCLUDED_)
