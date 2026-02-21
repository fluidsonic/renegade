#include "dlgsidebarhelp.h"
#include "mpsettingsmgr.h"
#include "resource.h"


//////////////////////////////////////////////////////////////////////
//
//	SidebarHelpDialogClass
//
//////////////////////////////////////////////////////////////////////
SidebarHelpDialogClass::SidebarHelpDialogClass (void)	:
	PopupDialogClass (IDD_SIDEBAR_HELP_POPUP)
{
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	~SidebarHelpDialogClass
//
//////////////////////////////////////////////////////////////////////
SidebarHelpDialogClass::~SidebarHelpDialogClass (void)
{
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
//////////////////////////////////////////////////////////////////////
void
SidebarHelpDialogClass::On_Init_Dialog (void)
{	
	Check_Dlg_Button (IDC_DONT_SHOW_AGAIN_CHECK, false);
	PopupDialogClass::On_Init_Dialog ();
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	On_Command
//
//////////////////////////////////////////////////////////////////////
void
SidebarHelpDialogClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	//
	//	Remember the "don't show me this again" setting
	//
	if (ctrl_id == IDOK) {
		bool is_checked = Is_Dlg_Button_Checked (IDC_DONT_SHOW_AGAIN_CHECK);
		MPSettingsMgrClass::Set_Is_Sidebar_Help_Displayed (!is_checked);

		//
		//	Close the dialog
		//
		End_Dialog ();
	}

	PopupDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}
