#if !defined(AFX_SPLASHSCREEN_H__9D3BD830_AC0C_11D3_A078_00104B791122__INCLUDED_)
#define AFX_SPLASHSCREEN_H__9D3BD830_AC0C_11D3_A078_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


/////////////////////////////////////////////////////////////////////////////
//
// SplashScreenClass
//
/////////////////////////////////////////////////////////////////////////////
class SplashScreenClass : public CWnd
{
// Construction
public:
	SplashScreenClass();

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(SplashScreenClass)
	protected:
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	virtual void PostNcDestroy();
	//}}AFX_VIRTUAL

// Implementation
public:
	virtual ~SplashScreenClass();

	// Generated message map functions
protected:
	//{{AFX_MSG(SplashScreenClass)
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	afx_msg void OnPaint();
	afx_msg void OnDestroy();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()


public:

	/////////////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////////////
	void				Set_Status_Text (LPCTSTR text);

protected:

	/////////////////////////////////////////////////////////////////////////////
	//	Protected methods
	/////////////////////////////////////////////////////////////////////////////
	void				Paint_Status_Text (HDC hdc);

private:

	/////////////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////////////
	HBITMAP	m_hBitmap;
	HDC		m_hMemDC;
	HFONT		m_hFont;
	CSize		m_Size;
	CString	m_StatusText;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SPLASHSCREEN_H__9D3BD830_AC0C_11D3_A078_00104B791122__INCLUDED_)
