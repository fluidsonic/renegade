#if !defined(AFX_SELECTPRESETDIALOG_H__8AB9B616_40C4_11D4_A0A6_00104B791122__INCLUDED_)
#define AFX_SELECTPRESETDIALOG_H__8AB9B616_40C4_11D4_A0A6_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "ntree.h"


///////////////////////////////////////////////////////////////////////
//	Forward declarations
///////////////////////////////////////////////////////////////////////
class PresetClass;


/////////////////////////////////////////////////////////////////////////////
//
// SelectPresetDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class SelectPresetDialogClass : public CDialog
{
// Construction
public:
	SelectPresetDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(SelectPresetDialogClass)
	enum { IDD = IDD_PRESET_SELECT };
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(SelectPresetDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(SelectPresetDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnOK();
	afx_msg void OnDestroy();
	afx_msg void OnSelchangedPresetTree(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnInfo();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:
	
	///////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////
	void				Set_Icon_Index (int index)			{ m_IconIndex = index; }
	void				Set_Class_ID (int class_id)		{ m_ClassID = class_id; }
	PresetClass *	Get_Selection (void) const			{ return m_Preset; }
	void				Set_Preset (PresetClass *preset)	{ m_Preset = preset; }

	//
	//	Selection control
	//
	void				Allow_None_Selection (bool onoff)	{ m_AllowNoneSelection = onoff; }

	//
	//	Dialog text control
	//
	void				Set_Title (LPCTSTR title)			{ m_Title = title; }
	void				Set_Message (LPCTSTR msg)			{ m_Message = msg; }
	
private:

	///////////////////////////////////////////////////////////////
	//	Private methods
	///////////////////////////////////////////////////////////////
	void				Fill_Tree (NTreeLeafClass<PresetClass *> *leaf, HTREEITEM parent_item);
	void				Generate_Tree (int class_id, HTREEITEM parent_item);
	void				Build_Full_Preset_Tree (void);
	
	///////////////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////////////
	bool				m_AllowNoneSelection;
	int				m_IconIndex;
	int				m_ClassID;
	PresetClass *	m_Preset;
	HTREEITEM		m_InitialSelection;

	CString			m_Title;
	CString			m_Message;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SELECTPRESETDIALOG_H__8AB9B616_40C4_11D4_A0A6_00104B791122__INCLUDED_)
