#if !defined(AFX_SKYPROPPAGE_H__CFBFA3F7_4179_4243_AA5E_47415502AC01__INCLUDED_)
#define AFX_SKYPROPPAGE_H__CFBFA3F7_4179_4243_AA5E_47415502AC01__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// SkyPropPage.h : header file
//

#include <afxdtctl.h>
#include "Resource.h"

/////////////////////////////////////////////////////////////////////////////
// SkyPropPageClass dialog

class SkyPropPageClass : public CPropertyPage
{
	DECLARE_DYNCREATE(SkyPropPageClass)

// Construction
public:
	SkyPropPageClass();
	~SkyPropPageClass();

// Dialog Data
	//{{AFX_DATA(SkyPropPageClass)
	enum { IDD = IDD_BACKGROUND_SKY };
	CSliderCtrl	CloudGloominessCtrl;
	CSliderCtrl	CloudCoverCtrl;
	CDateTimeCtrl	TimeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generate virtual function overrides
	//{{AFX_VIRTUAL(SkyPropPageClass)
	public:
	virtual void OnOK();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	// Generated message map functions
	//{{AFX_MSG(SkyPropPageClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnSkyLightMoon();
	afx_msg void OnSkyLightSun();
	afx_msg void OnSkyMoonFull();
	afx_msg void OnSkyMoonPartial();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

};


//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SKYPROPPAGE_H__CFBFA3F7_4179_4243_AA5E_47415502AC01__INCLUDED_)
