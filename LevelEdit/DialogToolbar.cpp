#include "StdAfx.H"
#include "DialogToolbar.H"
#include "AfxPriv.H"


BEGIN_MESSAGE_MAP(DialogToolbarClass, CToolBar)
	//{{AFX_MSG_MAP(DialogToolbarClass)
	ON_MESSAGE(WM_IDLEUPDATECMDUI, OnIdleUpdateCmdUI)
	ON_MESSAGE_VOID(WM_INITIALUPDATE, OnInitialUpdate)
	ON_NOTIFY_EX( TTN_NEEDTEXT, 0, OnNeedToolTipText)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


DialogToolbarClass::DialogToolbarClass (void)
	: CToolBar ()
{
	//{{AFX_DATA_INIT(DialogToolbarClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT

	return ;
}

#ifdef _DEBUG
void DialogToolbarClass::AssertValid() const
{
	CToolBar::AssertValid();
}

void DialogToolbarClass::Dump(CDumpContext& dc) const
{
	CToolBar::Dump(dc);
}
#endif //_DEBUG


///////////////////////////////////////////////////////////////////
//
//	Enable_Button
//
void
DialogToolbarClass::Enable_Button
(
	int id,
	bool benable
)
{
	// Get the button's style (we enable by using a style bit)
	int index = CommandToIndex (id);	
	UINT style = GetButtonStyle (index) & (~TBBS_DISABLED);
	
	// If we are disabling the button, set the 
	// disabled style bit.
	if (benable == false) {
		style |= TBBS_DISABLED;
		style &= ~TBBS_PRESSED;
	}

	// If the button isn't a separator, then modify its style
	if (!(style & TBBS_SEPARATOR)) {
		SetButtonStyle (index, style);
	}

	return ;
}


///////////////////////////////////////////////////////////////////
//
//	OnIdleUpdateCmdUI
//
LRESULT
DialogToolbarClass::OnIdleUpdateCmdUI (WPARAM, LPARAM)
{
	return 0L;
}


///////////////////////////////////////////////////////////////////
//
//	OnInitialUpdate
//
void
DialogToolbarClass::OnInitialUpdate (void)
{
	return ;
}


///////////////////////////////////////////////////////////////////
//
//	OnNotify
//
BOOL
DialogToolbarClass::OnNeedToolTipText
(
	UINT id,
	NMHDR *pTTTStruct,
	LRESULT *pResult
)
{
	if (pTTTStruct->code == TTN_NEEDTEXTA) {
		
		TOOLTIPTEXTA *ptooltip_info = (TOOLTIPTEXTA*)pTTTStruct;
		::lstrcpy (ptooltip_info->szText, "test");
	}

	(*pResult) = 0L;
	return TRUE;
}
