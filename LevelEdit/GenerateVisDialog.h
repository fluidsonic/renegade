#if !defined(AFX_GENERATEVISDIALOG_H__447D6F68_04A5_11D3_A025_00104B791122__INCLUDED_)
#define AFX_GENERATEVISDIALOG_H__447D6F68_04A5_11D3_A025_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// GenerateVisDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// GenerateVisDialogClass dialog

class GenerateVisDialogClass : public CDialog
{
// Construction
public:
	GenerateVisDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GenerateVisDialogClass)
	enum { IDD = IDD_AUTO_GEN_VIS };
	CSliderCtrl	m_SampleHeightSlider;
	CSliderCtrl	m_GranularitySlider;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GenerateVisDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GenerateVisDialogClass)
	virtual BOOL OnInitDialog();
	afx_msg void OnHScroll(UINT nSBCode, UINT nPos, CScrollBar* pScrollBar);
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GENERATEVISDIALOG_H__447D6F68_04A5_11D3_A025_00104B791122__INCLUDED_)
