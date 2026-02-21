#if defined(_MSC_VER)
#pragma once
#endif


#ifndef __DIALOGTOOLBAR_H
#define __DIALOGTOOLBAR_H

#include "Resource.H"

/////////////////////////////////////////////////////////////////
//
//	DialogToolbarClass
//
class DialogToolbarClass : public CToolBar
{
	public:
		DialogToolbarClass (void);
		virtual ~DialogToolbarClass (void) {}

// Form Data
public:
	//{{AFX_DATA(DialogToolbarClass)
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(DialogToolbarClass)
	protected:
	//}}AFX_VIRTUAL

// Implementation
protected:	
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(DialogToolbarClass)
	afx_msg LRESULT OnIdleUpdateCmdUI(WPARAM wParam, LPARAM lParam);
	afx_msg void OnInitialUpdate();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	//virtual BOOL OnNotify(WPARAM wParam, LPARAM lParam, LRESULT* pResult);
	BOOL OnNeedToolTipText (UINT id, NMHDR *pTTTStruct, LRESULT *pResult);



	public:
		
		///////////////////////////////////////////////////////
		//
		//	Public methods
		//
		void						Enable_Button (int id, bool benable = true);
};


#endif //__DIALOGTOOLBAR_H

