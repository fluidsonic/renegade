#if !defined(AFX_COVERSPOTINFOPAGE_H__2D902CA1_3328_11D4_A0A1_00104B791122__INCLUDED_)
#define AFX_COVERSPOTINFOPAGE_H__2D902CA1_3328_11D4_A0A1_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#ifndef __AFXEXT_H__
#include <afxext.h>
#endif


class CoverSpotNodeClass;
#include "DockableForm.h"
#include "resource.h"

/////////////////////////////////////////////////////////////////////////////
//
// CoverSpotInfoPageClass
//
/////////////////////////////////////////////////////////////////////////////
class CoverSpotInfoPageClass : public DockableFormClass
{
public:
	CoverSpotInfoPageClass (void);
	CoverSpotInfoPageClass (CoverSpotNodeClass *cover_spot);
	virtual ~CoverSpotInfoPageClass (void);

// Form Data
public:
	//{{AFX_DATA(CoverSpotInfoPageClass)
	enum { IDD = IDD_COVERSPOT_INFO };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CoverSpotInfoPageClass)
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
	//{{AFX_MSG(CoverSpotInfoPageClass)
	afx_msg void OnAddAttackLocation();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void						HandleInitDialog (void);
	bool						Apply_Changes (void);

protected:
	
	///////////////////////////////////////////////////////
	//	Inline accessors
	///////////////////////////////////////////////////////

private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	CoverSpotNodeClass *		m_CoverSpot;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_COVERSPOTINFOPAGE_H__2D902CA1_3328_11D4_A0A1_00104B791122__INCLUDED_)
