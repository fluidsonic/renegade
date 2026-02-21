#if !defined(AFX_PRESETGENERALTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_)
#define AFX_PRESETGENERALTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// PresetGeneralTab.h : header file
//

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif

#include "resource.h"
#include "dockableform.h"

// Forward declarations
class PresetClass;

/////////////////////////////////////////////////////////////////////////////
//
// PresetGeneralTabClass
//
/////////////////////////////////////////////////////////////////////////////
class PresetGeneralTabClass : public DockableFormClass
{

public:
	PresetGeneralTabClass (PresetClass *preset);
	virtual ~PresetGeneralTabClass (void);

// Form Data
public:
	//{{AFX_DATA(PresetGeneralTabClass)
	enum { IDD = IDD_PRESET_GENERAL_TAB };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(PresetGeneralTabClass)
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
	//{{AFX_MSG(PresetGeneralTabClass)
		// NOTE - the ClassWizard will add and remove member functions here.
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	/////////////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////////////
	void						HandleInitDialog (void);
	bool						Apply_Changes (void);

	bool						Is_Read_Only (void) const	{ return m_IsReadOnly; }
	void						Set_Read_Only (bool onoff)	{ m_IsReadOnly = onoff; }

private:

	/////////////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////////////
	PresetClass *			m_Preset;
	bool						m_IsReadOnly;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PRESETGENERALTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_)
