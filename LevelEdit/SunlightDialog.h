#if !defined(AFX_SUNLIGHTDIALOG_H__7822BACE_FDC4_11D2_A022_00104B791122__INCLUDED_)
#define AFX_SUNLIGHTDIALOG_H__7822BACE_FDC4_11D2_A022_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// SunlightDialog.h : header file
//

#include "Resource.H"
#include "Vector3.H"

class LightClass;

/////////////////////////////////////////////////////////////////////////////
// SunlightDialogClass dialog

class SunlightDialogClass : public CDialog
{
// Construction
public:
	SunlightDialogClass(CWnd* pParent = NULL);   // standard constructor
	~SunlightDialogClass (void);

// Dialog Data
	//{{AFX_DATA(SunlightDialogClass)
	enum { IDD = IDD_SUNLIGHT };
	CSliderCtrl	m_YawSlider;
	CSliderCtrl	m_PitchSlider;
	CSliderCtrl	m_IntensitySlider;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(SunlightDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(SunlightDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnHScroll(UINT nSBCode, UINT nPos, CScrollBar* pScrollBar);
	afx_msg void OnColor();
	afx_msg void OnDrawItem(int nIDCtl, LPDRAWITEMSTRUCT lpDrawItemStruct);
	afx_msg void OnUpdatePitchEdit();
	afx_msg void OnKillfocusPitchEdit();
	afx_msg void OnKillfocusYawEdit();
	afx_msg void OnUpdateYawEdit();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	private:
		Vector3					m_Color;
		LightClass *			m_pSunlight;
		float						m_Yaw;
		float						m_Pitch;
		float						m_Intensity;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SUNLIGHTDIALOG_H__7822BACE_FDC4_11D2_A022_00104B791122__INCLUDED_)
