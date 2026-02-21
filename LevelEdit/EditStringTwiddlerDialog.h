#if !defined(AFX_EDITSTRINGTWIDDLERDIALOG_H__05FF8A6C_8212_44F5_A971_94BE7B9A0F16__INCLUDED_)
#define AFX_EDITSTRINGTWIDDLERDIALOG_H__05FF8A6C_8212_44F5_A971_94BE7B9A0F16__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class StringTwiddlerClass;


/////////////////////////////////////////////////////////////////////////////
//
// EditStringTwiddlerDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class EditStringTwiddlerDialogClass : public CDialog
{
// Construction
public:
	EditStringTwiddlerDialogClass (CWnd *pParent = NULL);

// Dialog Data
	//{{AFX_DATA(EditStringTwiddlerDialogClass)
	enum { IDD = IDD_EDIT_STRING_TWIDDLER };
	CListCtrl	ListCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(EditStringTwiddlerDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(EditStringTwiddlerDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnAddButton();
	virtual void OnOK();
	afx_msg void OnKeydownListctrl(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()


public:

	//////////////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////////////
	void							Set_Twiddler (StringTwiddlerClass *twiddler)	{ StringObject = twiddler; }
	StringTwiddlerClass *	Get_Twiddler (void) const							{ return StringObject; }

private:

	//////////////////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////////////////
	void							Insert_String (int string_id);

	//////////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////////
	StringTwiddlerClass	*	StringObject;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_EDITSTRINGTWIDDLERDIALOG_H__05FF8A6C_8212_44F5_A971_94BE7B9A0F16__INCLUDED_)
