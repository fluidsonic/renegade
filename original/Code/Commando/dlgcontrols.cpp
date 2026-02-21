#include "dlgcontrols.h"
#include "dlgcontroltabs.h"
#include "dlgcontrolsaveload.h"
#include "resource.h"
#include "tabctrl.h"
#include "dialogresource.h"
#include "inputconfigmgr.h"

////////////////////////////////////////////////////////////////
//	Static member initialization
////////////////////////////////////////////////////////////////
ControlsMenuClass *	ControlsMenuClass::_TheInstance	= NULL;

////////////////////////////////////////////////////////////////
//
//	ControlsMenuClass
//
////////////////////////////////////////////////////////////////
ControlsMenuClass::ControlsMenuClass (void)	:
	MenuDialogClass (IDD_MENU_CONTROLS)
{
	_TheInstance = this;
	return ;
}

////////////////////////////////////////////////////////////////
//
//	~ControlsMenuClass
//
////////////////////////////////////////////////////////////////
ControlsMenuClass::~ControlsMenuClass (void)
{
	_TheInstance = NULL;
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
ControlsMenuClass::On_Init_Dialog (void)
{
	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_CONTROL_TABCTRL);
	if (tab_ctrl != NULL) {
		
		//
		//	Add the tabs to the control
		//
		TABCTRL_ADD_TAB (tab_ctrl, ControlsBasicMvmtTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, ControlsAttackTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, ControlsWeaponsTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, ControlsLookTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, ControlsMultiPlayTabClass);
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
ControlsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	switch (ctrl_id)
	{
		case IDCANCEL:
		case IDC_MENU_BACK_BUTTON:
			Apply_Changes ();
			break;

		case IDC_DEFAULTS_BUTTON:
			InputConfigMgrClass::Load_Default_Configuration ();
			break;

		case IDC_SAVELOAD_BUTTON:
			START_DIALOG (ControlSaveLoadMenuClass);
			break;
	}

	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Apply_Changes
//
////////////////////////////////////////////////////////////////
void
ControlsMenuClass::Apply_Changes (void)
{
	//
	//	Apply the changes on each tab
	//
	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_CONTROL_TABCTRL);
	if (tab_ctrl != NULL) {
		if (tab_ctrl->Apply_Changes_On_Tabs ()) {
			
			//
			//	Save the changes
			//
			InputConfigMgrClass::Save_Current_Configuration ();
		}
	}

	return; 
}

////////////////////////////////////////////////////////////////
//
//	Reload
//
////////////////////////////////////////////////////////////////
void
ControlsMenuClass::Reload (void)
{
	//
	//	Ask each tab to reload its data
	//
	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_CONTROL_TABCTRL);
	if (tab_ctrl != NULL) {
		tab_ctrl->Reload_Tabs ();
	}
	
	return ;
}

