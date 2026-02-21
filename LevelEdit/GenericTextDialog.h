#if !defined(AFX_GENERICTEXTDIALOG_H__DD4501B6_4F0A_4E92_AB87_16C5513E7E36__INCLUDED_)
#define AFX_GENERICTEXTDIALOG_H__DD4501B6_4F0A_4E92_AB87_16C5513E7E36__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"
#include "wwstring.h"


/////////////////////////////////////////////////////////////////////////////
//
// GenericTextDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class GenericTextDialogClass : public CDialog
{
// Construction
public:
	GenericTextDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GenericTextDialogClass)
	enum { IDD = IDD_GENERIC_TEXT_DIALOG };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GenericTextDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GenericTextDialogClass)
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	
	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	void				Set_Title (const char *title)			{ m_Title = title; }
	void				Set_Text (const char *text)			{ m_Text = text; }
	void				Set_Description (const char *desc)	{ m_Description = desc; }
	void				Set_Icon (int icon_id)					{ m_IconID = icon_id; }

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	StringClass		m_Title;
	StringClass		m_Text;
	StringClass		m_Description;
	int				m_IconID;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GENERICTEXTDIALOG_H__DD4501B6_4F0A_4E92_AB87_16C5513E7E36__INCLUDED_)
