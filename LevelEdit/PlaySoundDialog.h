#if !defined(AFX_PLAYSOUNDDIALOG_H__AD61372C_2DA3_11D3_A042_00104B791122__INCLUDED_)
#define AFX_PLAYSOUNDDIALOG_H__AD61372C_2DA3_11D3_A042_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// PlaySoundDialog.h : header file
//

#include "Resource.H"
#include "WWAudio.H"

/////////////////////////////////////////////////////////////////////////////
// PlaySoundDialogClass dialog

class PlaySoundDialogClass : public CDialog
{
// Construction
public:
	PlaySoundDialogClass(LPCTSTR filename, CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(PlaySoundDialogClass)
	enum { IDD = IDD_PLAY_SOUND_EFFECT };
		// NOTE: the ClassWizard will add data members here
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(PlaySoundDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(PlaySoundDialogClass)
	afx_msg void OnPlaySoundEffect();
	virtual void OnCancel();
	virtual BOOL OnInitDialog();
	afx_msg void OnStopSoundEffect();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	private:
		CString					m_Filename;
		AudibleSoundClass *	m_pSoundObj;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_PLAYSOUNDDIALOG_H__AD61372C_2DA3_11D3_A042_00104B791122__INCLUDED_)
