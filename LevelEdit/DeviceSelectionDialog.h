#if !defined(AFX_DEVICESELECTIONDIALOG_H__49E2DFF9_5D3F_11D2_9FC6_00104B791122__INCLUDED_)
#define AFX_DEVICESELECTIONDIALOG_H__49E2DFF9_5D3F_11D2_9FC6_00104B791122__INCLUDED_

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000
// DeviceSelectionDialog.h : header file
//

/////////////////////////////////////////////////////////////////////////////
// CDeviceSelectionDialog dialog

class CDeviceSelectionDialog : public CDialog
{
// Construction
public:
	CDeviceSelectionDialog(BOOL bLookupCachedInfo = TRUE, CWnd* pParent = NULL);   // standard constructor

// Dialog Data
	//{{AFX_DATA(CDeviceSelectionDialog)
	enum { IDD = IDD_RENDER_DEVICE_SELECTOR };
	CComboBox	m_deviceListComboBox;
	//}}AFX_DATA


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CDeviceSelectionDialog)
	public:
	virtual int DoModal();
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:

	// Generated message map functions
	//{{AFX_MSG(CDeviceSelectionDialog)
	virtual BOOL OnInitDialog();
	afx_msg void OnSelchangeRenderDeviceCombo();
	virtual void OnOK();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

    public:
        
        // Return the selected device index
        int GetDeviceIndex () const
            { return m_iDeviceIndex; }

        // Return the selected bits per pixel
        int GetBitsPerPixel () const
            { return m_iBitsPerPixel; }

        const CString &GetDriverName () const
            { return m_DriverName; }

    protected:
        void UpdateDeviceDescription (void);

    private:
        BOOL		m_bLookupCachedInfo;
        int			m_iDeviceIndex;
        int			m_iBitsPerPixel;
		  CString	m_DriverName;
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Developer Studio will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_DEVICESELECTIONDIALOG_H__49E2DFF9_5D3F_11D2_9FC6_00104B791122__INCLUDED_)
