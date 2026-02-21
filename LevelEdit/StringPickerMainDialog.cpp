// StringPickerMainDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "StringPickerMainDialog.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


/////////////////////////////////////////////////////////////////////////////
//
// StringPickerMainDialogClass
//
/////////////////////////////////////////////////////////////////////////////
StringPickerMainDialogClass::StringPickerMainDialogClass(CWnd* pParent /*=NULL*/)
	:	TextID (0),
		CDialog(StringPickerMainDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(StringPickerMainDialogClass)
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
StringPickerMainDialogClass::DoDataExchange (CDataExchange *pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(StringPickerMainDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(StringPickerMainDialogClass, CDialog)
	//{{AFX_MSG_MAP(StringPickerMainDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


/////////////////////////////////////////////////////////////////////////
//
//	OnInitDialog
//
/////////////////////////////////////////////////////////////////////////
BOOL
StringPickerMainDialogClass::OnInitDialog (void) 
{
	CDialog::OnInitDialog ();

	//
	//	Calculate the rectangle where we are to display the string picker
	//
	CRect rect;
	::GetWindowRect (::GetDlgItem (m_hWnd, IDC_PICKER_AREA), &rect);
	ScreenToClient (&rect);

	//
	//	Create the string picker
	//
	StringPicker.Set_Selection (TextID);
	StringPicker.Create (this);
	StringPicker.SetWindowPos (NULL, rect.left, rect.top, rect.Width (), rect.Height (), SWP_NOZORDER);
	StringPicker.ShowWindow (SW_SHOW);
	return TRUE;
}


/////////////////////////////////////////////////////////////////////////
//
//	OnOK
//
/////////////////////////////////////////////////////////////////////////
void
StringPickerMainDialogClass::OnOK (void)
{
	//
	//	Get the selected string from the picker control
	//
	TextID = StringPicker.Get_Selection ();

	CDialog::OnOK ();
	return ;
}
