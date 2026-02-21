// MissingTranslationReportDialog.cpp : implementation file
//

#include "stdafx.h"
#include "leveledit.h"
#include "missingtranslationreportdialog.h"
#include "reportmgr.h"
#include "translatedb.h"


#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// MissingTranslationReportDialogClass dialog


MissingTranslationReportDialogClass::MissingTranslationReportDialogClass(CWnd* pParent /*=NULL*/)
	: CDialog(MissingTranslationReportDialogClass::IDD, pParent)
{
	//{{AFX_DATA_INIT(MissingTranslationReportDialogClass)
		// NOTE: the ClassWizard will add member initialization here
	//}}AFX_DATA_INIT
	return ;
}


void
MissingTranslationReportDialogClass::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(MissingTranslationReportDialogClass)
		// NOTE: the ClassWizard will add DDX and DDV calls here
	//}}AFX_DATA_MAP
	return ;
}


BEGIN_MESSAGE_MAP(MissingTranslationReportDialogClass, CDialog)
	//{{AFX_MSG_MAP(MissingTranslationReportDialogClass)
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()


///////////////////////////////////////////////////////////////////////////
//
//	OnInitDialog
//
///////////////////////////////////////////////////////////////////////////
BOOL
MissingTranslationReportDialogClass::OnInitDialog (void) 
{
	CDialog::OnInitDialog ();
	
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
MissingTranslationReportDialogClass::OnOK (void) 
{
	//
	//	Import the data
	//
	int lang_id = SendDlgItemMessage (IDC_LANG_COMBO, CB_GETCURSEL);
	if (lang_id >= 0) {
		ReportMgrClass::Export_Missing_Translation_Report (Filename, lang_id);
	}
	
	CDialog::OnOK();
	return ;
}
