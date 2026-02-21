#include "dlgtechoptions.h"
#include "tabctrl.h"
#include "dlgconfigaudiotab.h"
#include "dlgconfigvideotab.h"
#include "dlgconfigperformancetab.h"
#include "systemsettings.h"


////////////////////////////////////////////////////////////////
//
//	TechOptionsMenuClass
//
////////////////////////////////////////////////////////////////
TechOptionsMenuClass::TechOptionsMenuClass (void)	:
	MenuDialogClass (IDD_OPTIONS_TECH)
{
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
TechOptionsMenuClass::On_Init_Dialog (void)
{
	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_TABCTRL);
	if (tab_ctrl != NULL) {
		
		//
		//	Add the tabs to the control
		//
		TABCTRL_ADD_TAB (tab_ctrl, DlgConfigAudioTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, DlgConfigVideoTabClass);		
		TABCTRL_ADD_TAB (tab_ctrl, DlgConfigPerformanceTabClass);
	}
		

	MenuDialogClass::On_Init_Dialog ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
TechOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	/*if (ctrl_id == IDOK) {

	}*/	

	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Destroy
//
////////////////////////////////////////////////////////////////
void
TechOptionsMenuClass::On_Destroy (void)
{
	//
	//	Get a pointer to the tab ctrl
	//
	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_TABCTRL);
	if (tab_ctrl != NULL) {

		//
		//	Save the changes on each tab of the control
		//
		tab_ctrl->Apply_Changes_On_Tabs ();
	}

	//
	//	Apply any system settings we modified
	//
	SystemSettings::Apply_All ();
	return ;
}
