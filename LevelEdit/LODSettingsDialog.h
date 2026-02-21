#if !defined(AFX_LODSETTINGSDIALOG_H__633E0B62_49FF_11D3_A050_00104B791122__INCLUDED_)
#define AFX_LODSETTINGSDIALOG_H__633E0B62_49FF_11D3_A050_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// LODSettingsDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// LODSettingsDialogClass dialog

class LODSettingsDialogClass : public CDialog
{
// Construction
public:
	LODSettingsDialogClass (unsigned int distance = 0, CWnd* pParent = NULL);

// Dialog Data
	//{{AFX_DATA(LODSettingsDialogClass)
	enum { IDD = IDD_LOD_SETTINGS };
	CSpinButtonCtrl	m_DistanceSpin;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(LODSettingsDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(LODSettingsDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:

		////////////////////////////////////////////////////////
		//	Public methods
		////////////////////////////////////////////////////////
		unsigned int		Get_Distance (void) const { return m_Distance; }

	private:

		////////////////////////////////////////////////////////
		//	Private member data
		////////////////////////////////////////////////////////
		unsigned int		m_Distance;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_LODSETTINGSDIALOG_H__633E0B62_49FF_11D3_A050_00104B791122__INCLUDED_)
