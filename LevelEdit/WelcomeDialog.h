#if !defined(AFX_WELCOMEDIALOG_H__FF90928E_9450_11D2_9FEB_00104B791122__INCLUDED_)
#define AFX_WELCOMEDIALOG_H__FF90928E_9450_11D2_9FEB_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// WelcomeDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// WelcomeDialogClass dialog

class WelcomeDialogClass : public CDialog
{
// Construction
public:
	WelcomeDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(WelcomeDialogClass)
	enum { IDD = IDD_WELCOME_PAGE };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(WelcomeDialogClass)
	public:
	virtual int DoModal();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(WelcomeDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnPaint();
	afx_msg void OnDestroy();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:
		bool					Are_Changes_Temporary (void) { return m_bChangesTemp; }

	private:
		bool					m_bChangesTemp;
		HBITMAP				m_hBMP;
		int					m_iWidth;
		int					m_iHeight;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_WELCOMEDIALOG_H__FF90928E_9450_11D2_9FEB_00104B791122__INCLUDED_)
