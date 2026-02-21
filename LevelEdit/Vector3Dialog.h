#if !defined(AFX_VECTOR3DIALOG_H__8AB9B61A_40C4_11D4_A0A6_00104B791122__INCLUDED_)
#define AFX_VECTOR3DIALOG_H__8AB9B61A_40C4_11D4_A0A6_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "resource.h"
#include "vector3.h"


/////////////////////////////////////////////////////////////////////////////
//
// Vector3DialogClass
//
/////////////////////////////////////////////////////////////////////////////
class Vector3DialogClass : public CDialog
{
public:
	
	Vector3DialogClass (CWnd *parent = NULL);

// Dialog Data
	//{{AFX_DATA(Vector3DialogClass)
	enum { IDD = IDD_VECTOR3 };
	CStatic	m_ZStatic;
	CStatic	m_YStatic;
	CStatic	m_XStatic;
	CEdit	m_ZEdit;
	CEdit	m_YEdit;
	CEdit	m_XEdit;
	CSpinButtonCtrl	m_ZSpin;
	CSpinButtonCtrl	m_YSpin;
	CSpinButtonCtrl	m_XSpin;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(Vector3DialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(Vector3DialogClass)
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	virtual BOOL OnInitDialog();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	///////////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////////
	
	//
	//	Creation
	//
	void			Create (CWnd *parent = NULL);

	//
	//	Data access
	//
	void			Set_Default_Value (const Vector3 &value);
	Vector3		Get_Current_Value (void);

private:

	///////////////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////////////
	Vector3		m_DefaultValue;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_VECTOR3DIALOG_H__8AB9B61A_40C4_11D4_A0A6_00104B791122__INCLUDED_)
