// LevelEdit.h : main header file for the LEVELEDIT application
//

#if !defined(AFX_LEVELEDIT_H__6D711A23_651E_11D2_9FC8_00104B791122__INCLUDED_)
#define AFX_LEVELEDIT_H__6D711A23_651E_11D2_9FC8_00104B791122__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

#ifndef __AFXWIN_H__
	#error include 'stdafx.h' before including this file for PCH
#endif

#include "resource.h"       // main symbols

/////////////////////////////////////////////////////////////////////////////
// CLevelEditApp:
// See LevelEdit.cpp for the implementation of this class
//

class CLevelEditApp : public CWinApp
{
public:
	CLevelEditApp();

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CLevelEditApp)
	public:
	virtual BOOL InitInstance();
	virtual int ExitInstance();
	virtual BOOL PreTranslateMessage(MSG* pMsg);
	//}}AFX_VIRTUAL

// Implementation

	//{{AFX_MSG(CLevelEditApp)
	afx_msg void OnAppAbout();
	afx_msg void OnFileOpen();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

extern CLevelEditApp theApp;

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_LEVELEDIT_H__6D711A23_651E_11D2_9FC8_00104B791122__INCLUDED_)
