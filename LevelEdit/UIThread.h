#if !defined(AFX_UITHREAD_H__A5E14BDA_8F04_11D2_9FE7_00104B791122__INCLUDED_)
#define AFX_UITHREAD_H__A5E14BDA_8F04_11D2_9FE7_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// UIThread.h : header file
//



/////////////////////////////////////////////////////////////////////////////
// UIThreadClass thread

class UIThreadClass : public CWinThread
{
	DECLARE_DYNCREATE(UIThreadClass)
protected:
	UIThreadClass();           // protected constructor used by dynamic creation

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(UIThreadClass)
	public:
	virtual BOOL InitInstance();
	virtual int ExitInstance();
	//}}AFX_VIRTUAL

// Implementation
protected:
	virtual ~UIThreadClass();

	// Generated message map functions
	//{{AFX_MSG(UIThreadClass)
		// NOTE - the ClassWizard will add and remove member functions here.
	//}}AFX_MSG

	DECLARE_MESSAGE_MAP()
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_UITHREAD_H__A5E14BDA_8F04_11D2_9FE7_00104B791122__INCLUDED_)
