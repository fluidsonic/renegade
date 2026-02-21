#if !defined(AFX_SOUNDSETTINGSPAGE_H__C0BCECAD_B6EA_11D2_9FF8_00104B791122__INCLUDED_)
#define AFX_SOUNDSETTINGSPAGE_H__C0BCECAD_B6EA_11D2_9FF8_00104B791122__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#ifndef __AFXEXT_H__
#include <afxext.h>
#endif


#include "dockableform.h"


//////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////
class AudibleSoundDefinitionClass;


//////////////////////////////////////////////////////////
//
//	SoundSettingsPageClass
//
//////////////////////////////////////////////////////////
class SoundSettingsPageClass : public DockableFormClass
{
	public:
		SoundSettingsPageClass (void);
		virtual ~SoundSettingsPageClass (void);

// Form Data
public:
	//{{AFX_DATA(SoundSettingsPageClass)
	enum { IDD = IDD_SOUND_SETTINGS };
	CSliderCtrl	m_VolumeSlider;
	//}}AFX_DATA

// Attributes
public:

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(SoundSettingsPageClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:	
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

	// Generated message map functions
	//{{AFX_MSG(SoundSettingsPageClass)
	afx_msg void OnHScroll(UINT nSBCode, UINT nPos, CScrollBar* pScrollBar);
	afx_msg void OnChangeVolumeEdit();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

public:

	///////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////
	void			HandleInitDialog (void);
	bool			Apply_Changes (void);
	void			Discard_Changes (void);

	//
	//	Accessors
	//
	void			Set_Definition (AudibleSoundDefinitionClass *definition)	{ Definition = definition; }

protected:
	
	///////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////
	void			Update_Enable_State (void);

private:

	///////////////////////////////////////////////////////
	//	Private member data
	///////////////////////////////////////////////////////
	AudibleSoundDefinitionClass *	Definition;
};

/////////////////////////////////////////////////////////////////////////////

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_SOUNDSETTINGSPAGE_H__C0BCECAD_B6EA_11D2_9FF8_00104B791122__INCLUDED_)
