#if !defined(AFX_PARAMETERINHERITANCEDIALOG_H__86AADEF7_408C_11D4_A0A6_00104B791122__INCLUDED_)
#define AFX_PARAMETERINHERITANCEDIALOG_H__86AADEF7_408C_11D4_A0A6_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "resource.h"
#include "vector.h"


/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class PresetClass;
class DefinitionClass;
class DefinitionParameterClass;


/////////////////////////////////////////////////////////////////////////////
//
// ParameterInheritanceDialogClass
//
/////////////////////////////////////////////////////////////////////////////
class ParameterInheritanceDialogClass : public CDialog
{
// Construction
public:
	ParameterInheritanceDialogClass (CWnd* pParent = NULL);

// Dialog Data
	//{{AFX_DATA(ParameterInheritanceDialogClass)
	enum { IDD = IDD_PARAMETER_INHERITANCE };
	CListCtrl	m_ListCtrl;
	CTreeCtrl	m_TreeCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(ParameterInheritanceDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(ParameterInheritanceDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnDestroy();
	virtual void OnOK();
	afx_msg void OnDeleteitemParameterList(NMHDR* pNMHDR, LRESULT* pResult);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	/////////////////////////////////////////////////////////////////////////////
	//	Public methods
	/////////////////////////////////////////////////////////////////////////////
	void					Set_Preset (PresetClass *preset)		{ m_Preset = preset; }

private:

	/////////////////////////////////////////////////////////////////////////////
	//	Private methods
	/////////////////////////////////////////////////////////////////////////////
	void					Populate_Tree_Ctrl (void);
	void					Add_Children_To_Tree (HTREEITEM parent_item, int parent_id);

	void					Populate_List_Ctrl (void);
	void					Add_Parameters_To_List (DefinitionClass *definition, DefinitionParameterClass *parent, int icon);

	void					Update_List_Entry_Check (int index, bool checked);
	void					Update_Tree_Entry_Check (HTREEITEM parent_item, bool checked);

	void					Propagate_Changes (DefinitionClass *base_def, DefinitionClass *derived_def, DynamicVectorClass<DefinitionParameterClass *> &parameter_list);
	void					Build_Parameter_List (DynamicVectorClass<DefinitionParameterClass *> &parameter_list);
	void					Build_Preset_List (HTREEITEM parent_item, DynamicVectorClass<PresetClass *> &preset_list);


	/////////////////////////////////////////////////////////////////////////////
	//	Private member data
	/////////////////////////////////////////////////////////////////////////////
	PresetClass *		m_Preset;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PARAMETERINHERITANCEDIALOG_H__86AADEF7_408C_11D4_A0A6_00104B791122__INCLUDED_)
