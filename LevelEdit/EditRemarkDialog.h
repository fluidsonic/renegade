#if !defined(AFX_EDITREMARKDIALOG_H__6A88EE92_3F48_4C4D_A4EA_5C519713E6A9__INCLUDED_)
#define AFX_EDITREMARKDIALOG_H__6A88EE92_3F48_4C4D_A4EA_5C519713E6A9__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "stringpickerdialog.h"


/////////////////////////////////////////////////////////////////////////////
// Forward declarations
/////////////////////////////////////////////////////////////////////////////
class DialogueRemarkClass;


/////////////////////////////////////////////////////////////////////////////
//
// EditRemarkDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditRemarkDialogClass : public CDialog
{
// Construction
public:
	EditRemarkDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(EditRemarkDialogClass)
	enum { IDD = IDD_EDIT_REMARK };
	CSpinButtonCtrl	m_WeightSpin;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditRemarkDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditRemarkDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	////////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////////
	void							Set_Remark (DialogueRemarkClass *remark)	{ m_Remark = remark; }
	DialogueRemarkClass *	Get_Remark (void) const							{ return m_Remark; }

private:

	////////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////////
	DialogueRemarkClass *	m_Remark;
	StringPickerDialogClass StringPicker;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITREMARKDIALOG_H__6A88EE92_3F48_4C4D_A4EA_5C519713E6A9__INCLUDED_)
