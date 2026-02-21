#if !defined(AFX_IMPORTTRANSLATIONDIALOG_H__A93FE99F_6FD4_4478_A5AF_13C2A91C8F7B__INCLUDED_)
#define AFX_IMPORTTRANSLATIONDIALOG_H__A93FE99F_6FD4_4478_A5AF_13C2A91C8F7B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ImportTranslationDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// ImportTranslationDialogClass dialog

class ImportTranslationDialogClass : public CDialog
{
// Construction
public:
	ImportTranslationDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(ImportTranslationDialogClass)
	enum { IDD = IDD_IMPORT_TRANSLATION };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ImportTranslationDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ImportTranslationDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	//////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////
	void				Set_Filename (const char *filename)	{ Filename = filename; }
	const char *	Get_Filename (void) const				{ return Filename; }

	void				Set_Is_For_Export (bool onoff)		{ IsForExport = onoff; }

private:

	//////////////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////////////
	CString		Filename;
	bool			IsForExport;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_IMPORTTRANSLATIONDIALOG_H__A93FE99F_6FD4_4478_A5AF_13C2A91C8F7B__INCLUDED_)
