#if !defined(AFX_STRINGLIBRARYDIALOG_H__D16AE211_5388_11D4_A0A6_00104B791122__INCLUDED_)
#define AFX_STRINGLIBRARYDIALOG_H__D16AE211_5388_11D4_A0A6_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "vector.h"
#include "stringscategoryviewdialog.h"


/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class TDBCategoryClass;
class TDBObjClass;

/////////////////////////////////////////////////////////////////////////////
//
// StringLibraryDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class StringLibraryDialogClass : public CDialog, public StringsCategoryViewCallbackClass
{
// Construction
public:
	StringLibraryDialogClass (CWnd *pParent = NULL);
	~StringLibraryDialogClass (void);

// Dialog Data
	//{{AFX_DATA(StringLibraryDialogClass)
	enum { IDD = IDD_STRING_LIBRARY };
	CTabCtrl	m_TabCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(StringLibraryDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(StringLibraryDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnSize(UINT nType, int cx, int cy);
	virtual void OnOK();
	afx_msg void OnSelchangeTabCtrl(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnAdd();
	afx_msg void OnRemove();
	afx_msg void OnRename();
	afx_msg void OnModeTwiddler();
	afx_msg void OnModeString();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////
	void	On_Cut (void);
	void	On_Paste (void);
	void	Clear_Clipboard (void);

protected:

	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	void	Resize_Controls (void);

private:

	///////////////////////////////////////////////////////////////////
	//	Private constants
	///////////////////////////////////////////////////////////////////
	typedef enum
	{
		MODE_STRING		= 0,
		MODE_TWIDDLER
	} MODE;
	
	///////////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////////
	void	Enable_Buttons (void);
	void	Add_Category_Page (TDBCategoryClass *category);
	void	Update_Page_Visibility (void);
	void	Update_Mode (void);

	///////////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////////
	bool							IsInitialized;
	bool							ShouldUpdateVersionNumber;
	int							CurrentTab;
	MODE							Mode;

	DynamicVectorClass<TDBObjClass *>							LocalClipboard;
	DynamicVectorClass<StringsCategoryViewDialogClass *>	CategoryPages;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_STRINGLIBRARYDIALOG_H__D16AE211_5388_11D4_A0A6_00104B791122__INCLUDED_)
