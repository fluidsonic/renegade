////////////////////////////////////////////////////////////////////////
//
//	FormToolbarClass.H
//
//	Module declaring a CControlBar derived class which is used to implement
// floating or dockable toolbars containing dialogs.
//
//

#ifndef __FORMTOOLBARCLASS_H
#define __FORMTOOLBARCLASS_H

//////////////////////////////////////////////////////////////
//
//  FormToolbarClass
//
class FormToolbarClass : public CControlBar
{

// Operations
public:

// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(FormToolbarClass)
	protected:
	//}}AFX_VIRTUAL

// Implementation

	// Generated message map functions
	//{{AFX_MSG(FormToolbarClass)
	afx_msg void OnSize(UINT nType, int cx, int cy);
	afx_msg BOOL OnEraseBkgnd(CDC* pDC);
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()

    public:

        ////////////////////////////////////////////////////////
        //
        //  Public Contructors
        //
        FormToolbarClass ();        
        virtual ~FormToolbarClass ();


        ////////////////////////////////////////////////////////
        //
        //  Public Methods
        //

        //
        //  Required methods
        //
        CSize CalcFixedLayout (BOOL, BOOL) { return CSize (100, 100); }
        CSize CalcDynamicLayout( int nLength, DWORD dwMode )
        {
            if (dwMode & LM_VERTDOCK)
            {                
                if (::AfxGetMainWnd () &&
                    ::AfxGetMainWnd ()->GetDlgItem (AFX_IDW_PANE_FIRST))
                {
							CRect rect;
                    ::AfxGetMainWnd ()->GetDlgItem (AFX_IDW_PANE_FIRST)->GetClientRect (&rect);
                    return CSize (m_minSize.cx, rect.Height ()+8);
                }
                else
                {
                    return m_minSize;
                }
            }
            else if (dwMode & LM_HORZDOCK)
            {                
                if (::AfxGetMainWnd ())
                {
							CRect rect;
                    ::AfxGetMainWnd ()->GetClientRect (&rect);
                    return CSize (rect.Width ()+8, m_minSize.cy);
                }
                else
                {
                    return m_minSize;
                }
            }
            else
            {
                return m_minSize;
            }
        }

        void									OnUpdateCmdUI (class CFrameWnd*, int) {}
        BOOL									Create (class DockableFormClass *pCFormClass, LPCTSTR pszWindowName, CWnd *pCParentWnd, UINT uiID);        

		  class DockableFormClass *		Get_Form (void) const { return m_pCForm; }

	protected:

	private:

		////////////////////////////////////////////////////////
		//
		//  Private member data
		//
		class DockableFormClass *m_pCForm;
		CSize m_minSize;              
};


#endif //__FORMTOOLBARCLASS_H
