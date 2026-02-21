#if !defined(AFX_SCRIPTEDITDIALOG_H__39A766E6_8CB0_11D3_A06A_00104B791122__INCLUDED_)
#define AFX_SCRIPTEDITDIALOG_H__39A766E6_8CB0_11D3_A06A_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


#include "..\..\scripts\scriptevents.h"
#include "EditScript.h"
#include "Resource.h"
#include "Vector3Dialog.h"


/////////////////////////////////////////////////////////////////////////////
//
// ScriptEditDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class ScriptEditDialogClass : public CDialog
{
// Construction
public:
	ScriptEditDialogClass (CWnd* pParent = NULL);
	~ScriptEditDialogClass (void);

// Dialog Data
	//{{AFX_DATA(ScriptEditDialogClass)
	enum { IDD = IDD_PRESET_SCRIPT };
	CEdit					m_ValueStringEdit;
	CSpinButtonCtrl	m_ValueNumberSpin;
	CEdit					m_ValueNumberEdit;
	CComboBox			m_ValueListCombo;
	CButton				m_ValueBoolCheck;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ScriptEditDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ScriptEditDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnSelChangeParamNameCombo();
	afx_msg void OnSelChangeScriptName();
	afx_msg void OnDeltaposParamValueNumberSpin(NMHDR* pNMHDR, LRESULT* pResult);
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void						Set_Script (const EditScriptClass &script)	{ m_Script = script; }
	EditScriptClass &		Get_Script (void)										{ return m_Script; }

protected:

	///////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////
	void						Fill_Param_Combo (void);
	void						Switch_Value_Control_Type (int param_index);
	void						Switch_Value_Control_Type (void);
	void						Save_Current_Value (void);
	void						Fill_Value_Control (LPCTSTR value);


private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	PARAM_TYPES				m_CurrentParamType;
	int						m_CurrentParamIndex;
	EditScriptClass		m_Script;

	Vector3DialogClass *	m_ValueVector3Dlg;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SCRIPTEDITDIALOG_H__39A766E6_8CB0_11D3_A06A_00104B791122__INCLUDED_)
