#if !defined(AFX_MISSINGTRANSLATIONREPORTDIALOG_H__6A3702A2_485A_4F65_864A_6BD300BF1786__INCLUDED_)
#define AFX_MISSINGTRANSLATIONREPORTDIALOG_H__6A3702A2_485A_4F65_864A_6BD300BF1786__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"


class MissingTranslationReportDialogClass : public CDialog
{
// Construction
public:
	MissingTranslationReportDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(MissingTranslationReportDialogClass)
	enum { IDD = IDD_EXPORT_MISSING_TRANSLATIONS };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(MissingTranslationReportDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(MissingTranslationReportDialogClass)
	virtual void OnOK();
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////
	void				Set_Filename (const char *filename)	{ Filename = filename; }
	const char *	Get_Filename (void) const				{ return Filename; }

private:

	//////////////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////////////
	CString		Filename;

};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_MISSINGTRANSLATIONREPORTDIALOG_H__6A3702A2_485A_4F65_864A_6BD300BF1786__INCLUDED_)
