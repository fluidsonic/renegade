#if !defined(AFX_UPDATEPRESETDIALOG_H__E2337FFD_C5CF_11D2_9FFA_00104B791122__INCLUDED_)
#define AFX_UPDATEPRESETDIALOG_H__E2337FFD_C5CF_11D2_9FFA_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "listtypes.h"

//
// Forward delcarations
//
class PresetClass;
class SpecSheetClass;


/////////////////////////////////////////////////////////////////////////////
//
// UpdatePresetDialogClass dialog
//
/////////////////////////////////////////////////////////////////////////////
class UpdatePresetDialogClass : public CDialog
{
// Construction
public:
	UpdatePresetDialogClass (PresetClass *preset, CWnd* pParent = NULL);

// Dialog Data
	//{{AFX_DATA(UpdatePresetDialogClass)
	enum { IDD = IDD_UPDATE_VSS_ASSET };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(UpdatePresetDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(UpdatePresetDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnBrowse();
	afx_msg void OnChangeFilenameEdit();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

protected:

	//////////////////////////////////////////////////////////////////////
	//	Protected  methods
	//////////////////////////////////////////////////////////////////////
	void					Update_INI (LPCTSTR rel_folder);

private:

	//////////////////////////////////////////////////////////////////////
	//	Private member data
	//////////////////////////////////////////////////////////////////////
	PresetClass *		m_Preset;
	CString				m_ModelFileName;
	CString				m_ModelLocalPath;

	STRING_LIST			m_LocalFileList;

	SpecSheetClass *	m_ParamSheet;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_UPDATEPRESETDIALOG_H__E2337FFD_C5CF_11D2_9FFA_00104B791122__INCLUDED_)
