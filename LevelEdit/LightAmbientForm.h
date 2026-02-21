#if !defined(AFX_LIGHTAMBIENTFORMCLASS_H__11C18BF8_759F_11D2_9FD0_00104B791122__INCLUDED_)
#define AFX_LIGHTAMBIENTFORMCLASS_H__11C18BF8_759F_11D2_9FD0_00104B791122__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000
// LightAmbientFormClass.h : header file
//

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif


#include "DockableForm.H"
#include "resource.h"

/////////////////////////////////////////////////////////////////
//
//	LightAmbientFormClass
//
class LightAmbientFormClass : public DockableFormClass
{
	public:
		LightAmbientFormClass (void);
		virtual ~LightAmbientFormClass (void);

// Form Data
public:
	//{{AFX_DATA(LightAmbientFormClass)
	enum { IDD = IDD_LIGHT_AMBIENT_DIALOG };
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(LightAmbientFormClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:	
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(LightAmbientFormClass)
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()


	public:

		///////////////////////////////////////////////////////
		//
		//	Public methods
		//
		void			HandleInitDialog (void);
		void			Update_Settings (void);

	private:

		///////////////////////////////////////////////////////
		//
		//	Private member data
		//
		int	m_InitialRed;
		int	m_InitialGreen;
		int	m_InitialBlue;
		HWND	m_ColorForm;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_LIGHTAMBIENTFORMCLASS_H__11C18BF8_759F_11D2_9FD0_00104B791122__INCLUDED_)
