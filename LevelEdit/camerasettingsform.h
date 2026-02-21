#if !defined(AFX_AMERASETTINGSFORMCLASS_H__646751E5_7A54_11D2_9FD2_00104B791122__INCLUDED_)
#define AFX_AMERASETTINGSFORMCLASS_H__646751E5_7A54_11D2_9FD2_00104B791122__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000
// ameraSettingsFormClass.h : header file
//

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif


#include "DockableForm.H"
#include "Resource.H"


/////////////////////////////////////////////////////////////////
//
//	CameraSettingsFormClass
//
class CameraSettingsFormClass : public DockableFormClass
{
	public:
		CameraSettingsFormClass ();
		virtual ~CameraSettingsFormClass (void);

// Form Data
public:
	//{{AFX_DATA(CameraSettingsFormClass)
	enum { IDD = IDD_CAMERA_SETTINGS };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CameraSettingsFormClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(CameraSettingsFormClass)
	afx_msg void OnDeltaPosDepthSpin(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnUpdateDepthEdit();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:

		///////////////////////////////////////////////////////
		//
		//	Public methods
		//
		void		HandleInitDialog (void);
		void		Update_Controls (void);

	protected:

		///////////////////////////////////////////////////////
		//
		//	Private member data
		//
		void		Set_Depth (int new_depth);

	private:

		///////////////////////////////////////////////////////
		//
		//	Private member data
		//
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_AMERASETTINGSFORMCLASS_H__646751E5_7A54_11D2_9FD2_00104B791122__INCLUDED_)
