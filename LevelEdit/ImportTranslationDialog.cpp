// ImportTranslationDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "importtranslationdialog.h"
#include "stringsmgr.h"
#include "translatedb.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif


///////////////////////////////////////////////////////////////////////////
//
//	ImportTranslationDialogClass
//
///////////////////////////////////////////////////////////////////////////
ImportTranslationDialogClass::ImportTranslationDialogClass (CWnd* pParent /*=NULL*/)
	:	IsForExport (false),
		CDialog(ImportTranslationDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(ImportTranslationDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


///////////////////////////////////////////////////////////////////////////
//
//	DoDataExchange
//
///////////////////////////////////////////////////////////////////////////
void
ImportTranslationDialogClass::DoDataExchange (CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(ImportTranslationDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(ImportTranslationDialogClass, CDialog)
	//{{AFX_MSG_MAP(ImportTranslationDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


///////////////////////////////////////////////////////////////////////////
//
//	OnInitDialog
//
///////////////////////////////////////////////////////////////////////////
BOOL
ImportTranslationDialogClass::OnInitDialog (void) 
{
	CDialog::OnInitDialog ();

	if (IsForExport) {
		SetDlgItemText (IDC_MESSAGE, "Which language are you exporting?");
		SetWindowText ("Translation Export");
	}

	//
	//	Select english by default
	//
	SendDlgItemMessage (IDC_LANG_COMBO, CB_SETCURSEL, TranslateDBClass::LANGID_ENGLISH);	
	return TRUE;
}


///////////////////////////////////////////////////////////////////////////
//
//	OnOK
//
///////////////////////////////////////////////////////////////////////////
void
ImportTranslationDialogClass::OnOK (void)
{
	if (IsForExport) {

		//
		//	Export the data
		//
		int lang_id = SendDlgItemMessage (IDC_LANG_COMBO, CB_GETCURSEL);
		if (lang_id >= 0) {
			StringsMgrClass::Export_For_Translation (Filename, lang_id);
		}
		
	} else {
		
		//
		//	Import the data
		//
		int lang_id = SendDlgItemMessage (IDC_LANG_COMBO, CB_GETCURSEL);
		if (lang_id >= 0) {
			StringsMgrClass::Import_From_Translation (Filename, lang_id);
		}
	}

	CDialog::OnOK ();
	return ;
}
