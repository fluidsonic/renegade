#if !defined(AFX_HEIGHTFIELDMATERIALSETTINGSDIALOG_H__E4621EB8_6CB2_4BEC_A103_1FD3525618E3__INCLUDED_)
#define AFX_HEIGHTFIELDMATERIALSETTINGSDIALOG_H__E4621EB8_6CB2_4BEC_A103_1FD3525618E3__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// HeightfieldMaterialSettingsDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// HeightfieldMaterialSettingsDialogClass dialog

class HeightfieldMaterialSettingsDialogClass : public CDialog
{
// Construction
public:
	HeightfieldMaterialSettingsDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(HeightfieldMaterialSettingsDialogClass)
	enum { IDD = IDD_HEIGHTFIELD_MATERIAL_SETTINGS };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(HeightfieldMaterialSettingsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(HeightfieldMaterialSettingsDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnBrowse();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Material access
	//
	void				Set_Material (int index)	{ MaterialIndex = index; }

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	int	MaterialIndex;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_HEIGHTFIELDMATERIALSETTINGSDIALOG_H__E4621EB8_6CB2_4BEC_A103_1FD3525618E3__INCLUDED_)
