// EditConversationRemarkDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "editconversationremarkdialog.h"
#include "stringsmgr.h"
#include "translatedb.h"
#include "translateobj.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//	Constants
/////////////////////////////////////////////////////////////////////////////

static const int	ORATOR_MAX	= 7;

typedef struct _ORATOR_BUTTON_INFO
{
	int	button_id;
	int	icon_id;
} ORATOR_BUTTON_INFO;

const ORATOR_BUTTON_INFO ORATOR_BUTTONS[ORATOR_MAX] =
{
	{ IDC_ORATOR1_RADIO, IDI_ORATOR1 },
	{ IDC_ORATOR2_RADIO, IDI_ORATOR2 },
	{ IDC_ORATOR3_RADIO, IDI_ORATOR3 },
	{ IDC_ORATOR4_RADIO, IDI_ORATOR4 },
	{ IDC_ORATOR5_RADIO, IDI_ORATOR5 },
	{ IDC_ORATOR6_RADIO, IDI_ORATOR6 },
	{ IDC_ORATOR7_RADIO, IDI_ORATOR7 }
};


/////////////////////////////////////////////////////////////////////////////
//
// EditConversationRemarkDialogClass
//
/////////////////////////////////////////////////////////////////////////////
EditConversationRemarkDialogClass::EditConversationRemarkDialogClass(CWnd* pParent /*=NULL*/)
	:	m_OratorBitmask (1),
		CDialog(EditConversationRemarkDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(EditConversationRemarkDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


/////////////////////////////////////////////////////////////////////////////
//
// DoDataExchange
//
/////////////////////////////////////////////////////////////////////////////
void
EditConversationRemarkDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(EditConversationRemarkDialogClass)
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(EditConversationRemarkDialogClass, CDialog)
	//{{AFX_MSG_MAP(EditConversationRemarkDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////////
//
// OnInitDialog
//
/////////////////////////////////////////////////////////////////////////////
BOOL
EditConversationRemarkDialogClass::OnInitDialog (void)
{
	CDialog::OnInitDialog ();

	//
	//	Assign icons to each of the orator buttons
	//
	for (int index = 0; index < ORATOR_MAX; index ++) {		
		HICON icon = ::LoadIcon (::AfxGetResourceHandle (), MAKEINTRESOURCE (ORATOR_BUTTONS[index].icon_id));
		SendDlgItemMessage (ORATOR_BUTTONS[index].button_id, BM_SETIMAGE, IMAGE_ICON, (LPARAM)icon);

		//
		//	Disable this button if the user hasn't configured the orator
		//
		if ((m_OratorBitmask & (1 << index)) == false) {
			::EnableWindow (::GetDlgItem (m_hWnd, ORATOR_BUTTONS[index].button_id), FALSE);
		}
	}

	//
	//	Select the current orator
	//
	SendDlgItemMessage (ORATOR_BUTTONS[Remark.Get_Orator_ID ()].button_id, BM_SETCHECK, TRUE);

	//
	//	Put the animation name into the text field
	//
	SetDlgItemText (IDC_ANIMATION_NAME, Remark.Get_Animation_Name ());

	//
	//	Calculate the rectangle where we are to display the string picker
	//
	CRect rect;
	::GetWindowRect (::GetDlgItem (m_hWnd, IDC_PICKER_AREA), &rect);
	ScreenToClient (&rect);

	//
	//	Create the string picker
	//
	StringPicker.Set_Selection (Remark.Get_Text_ID ());
	StringPicker.Create (this);
	StringPicker.SetWindowPos (NULL, rect.left, rect.top, rect.Width (), rect.Height (), SWP_NOZORDER);
	StringPicker.ShowWindow (SW_SHOW);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////////
//
// OnOK
//
/////////////////////////////////////////////////////////////////////////////
void
EditConversationRemarkDialogClass::OnOK (void)
{
	//
	//	Assign icons to each of the orator buttons
	//
	for (int index = 0; index < ORATOR_MAX; index ++) {		
		if (SendDlgItemMessage (ORATOR_BUTTONS[index].button_id, BM_GETCHECK) == 1) {
			Remark.Set_Orator_ID (index);
			break;
		}		
	}

	//
	//	Get the selected string from the picker object
	//
	Remark.Set_Text_ID (StringPicker.Get_Selection ());

	//
	//	Get the animation name from the text field
	//
	CString animation_name;
	GetDlgItemText (IDC_ANIMATION_NAME, animation_name);

	//
	//	Pass the animation name onto the remark object 
	//
	Remark.Set_Animation_Name (animation_name);

	CDialog::OnOK ();
	return ;
}

