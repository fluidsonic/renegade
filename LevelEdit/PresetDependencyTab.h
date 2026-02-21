#if !defined(AFX_PRESETDEPENDENCYTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_)
#define AFX_PRESETDEPENDENCYTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// PresetDependencyTab.h : header file
//

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif

#include "resource.h"
#include "dockableform.h"

/////////////////////////////////////////////////////////////////////////////
// Forward declarations
/////////////////////////////////////////////////////////////////////////////
class PresetClass;


/////////////////////////////////////////////////////////////////////////////
//
// PresetDependencyTabClass
//
/////////////////////////////////////////////////////////////////////////////
class PresetDependencyTabClass : public DockableFormClass
{

public:
	PresetDependencyTabClass (PresetClass *preset);
	virtual ~PresetDependencyTabClass (void);

// Form Data
public:
	//{{AFX_DATA(PresetDependencyTabClass)
	enum { IDD = IDD_PRESET_DEPENDENCIES_TAB };
	CListCtrl	m_ListCtrl;
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(PresetDependencyTabClass)
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
	//{{AFX_MSG(PresetDependencyTabClass)
	afx_msg void OnAdd();
	afx_msg void OnRemove();
	afx_msg void OnItemchangedDepencyList(NMHDR* pNMHDR, LRESULT* pResult);
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

protected:
	
	/////////////////////////////////////////////////////////////////////////////
	//	Protected methods
	/////////////////////////////////////////////////////////////////////////////
	void						Update_Button_State (void);

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

#endif // !defined(AFX_PRESETDEPENDENCYTAB_H__9CC8BAE8_7052_11D3_A05A_00104B791122__INCLUDED_)
