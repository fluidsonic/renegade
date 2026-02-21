#if !defined(AFX_GENERATINGLIGHTVISDIALOG_H__489C0AF7_09BD_46FF_A99B_F885AEC4C9C6__INCLUDED_)
#define AFX_GENERATINGLIGHTVISDIALOG_H__489C0AF7_09BD_46FF_A99B_F885AEC4C9C6__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// GeneratingLightVisDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// GeneratingLightVisDialogClass dialog

class GeneratingLightVisDialogClass : public CDialog
{
// Construction
public:
	GeneratingLightVisDialogClass(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(GeneratingLightVisDialogClass)
	enum { IDD = IDD_GENERATING_LIGHT_VIS };
	CProgressCtrl	m_ProgressCtrl;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(GeneratingLightVisDialogClass)
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	virtual LRESULT WindowProc(UINT message, WPARAM wParam, LPARAM lParam);
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(GeneratingLightVisDialogClass)
	virtual BOOL OnInitDialog();
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

	public:
		
		/////////////////////////////////////////////////////////////////////////////////
		//	Public methods
		/////////////////////////////////////////////////////////////////////////////////
		void			Set_Farm_Mode (int index, int total) { m_ProcessorIndex = index; m_TotalProcessors = total; m_FarmMode = true; }

	protected:

		/////////////////////////////////////////////////////////////////////////////////
		//	Protected methods
		/////////////////////////////////////////////////////////////////////////////////
		void			Update_Status (int cur_light);

	private:
		
		/////////////////////////////////////////////////////////////////////////////////
		//	Private member data
		/////////////////////////////////////////////////////////////////////////////////
		bool			m_IsCancelled;
		DWORD			m_StartTicks;
		int			m_CurrentLight;
		int			m_FirstLight;
		int			m_LastLight;

		bool			m_FarmMode;
		int			m_TotalProcessors;
		int			m_ProcessorIndex;
		CString		m_StatusFilename;
		CString		m_StatusSection;
		
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_GENERATINGLIGHTVISDIALOG_H__489C0AF7_09BD_46FF_A99B_F885AEC4C9C6__INCLUDED_)
