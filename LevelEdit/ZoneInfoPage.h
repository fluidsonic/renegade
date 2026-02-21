#if !defined(AFX_ZONEINFOPAGE_H__B17C4184_A438_11D2_9FEE_00104B791122__INCLUDED_)
#define AFX_ZONEINFOPAGE_H__B17C4184_A438_11D2_9FEE_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// ZoneInfoPage.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// ZoneInfoPageClass form view

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif

class ZoneInstanceClass;
#include "DockableForm.H"


//////////////////////////////////////////////////////////
//
//	ZoneInfoPageClass
//
class ZoneInfoPageClass : public DockableFormClass
{
	public:
		ZoneInfoPageClass ();
		ZoneInfoPageClass (ZoneInstanceClass *pzone);
		virtual ~ZoneInfoPageClass ();

// Form Data
public:
	//{{AFX_DATA(ZoneInfoPageClass)
	enum { IDD = IDD_ZONE_INFO };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ZoneInfoPageClass)
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
	//{{AFX_MSG(ZoneInfoPageClass)
		// NOTE - the ClassWizard will add and remove member functions here.
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:

		///////////////////////////////////////////////////////
		//
		//	Public methods
		//
		void						HandleInitDialog (void);
		bool						Apply_Changes (void);


		///////////////////////////////////////////////////////
		//
		//	Inline accessors
		//
		void						Set_Zone (ZoneInstanceClass *pzone) { m_pZone = pzone; }
		ZoneInstanceClass *	Get_Zone (void) { return m_pZone; }


	private:

		///////////////////////////////////////////////////////
		//
		//	Private member data
		//
		ZoneInstanceClass *m_pZone;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_ZONEINFOPAGE_H__B17C4184_A438_11D2_9FEE_00104B791122__INCLUDED_)
