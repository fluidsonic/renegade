#include "dlgmultiplayoptions.h"
#include "useroptions.h"
#include "vector3.h"
#include "listctrl.h"
#include "checkboxctrl.h"
#include "dialogresource.h"
#include "cnetwork.h"
#include "gamemode.h"
#include "netinterface.h"
#include "mpsettingsmgr.h"
#include "registry.h"
#include "_globals.h"

////////////////////////////////////////////////////////////////
//
//	MultiplayOptionsMenuClass
//
////////////////////////////////////////////////////////////////
MultiplayOptionsMenuClass::MultiplayOptionsMenuClass (void)	:
	MenuDialogClass (IDD_MULTIPLAY_OPTIONS)
{
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MultiplayOptionsMenuClass::On_Init_Dialog (void)
{
	//
	//	Configure the checkboxes
	//
	Check_Dlg_Button (IDC_DISPLAY_PLAYER_NAMES_CHECK, cUserOptions::ShowNamesOnSoldier.Is_True ());

	//
	//	Configure the internet options
	//
	Check_Dlg_Button (IDC_ALLOW_PAGES_CHECK,			MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_ALLOW_PAGES));
	Check_Dlg_Button (IDC_LANGUAGE_FILTER_CHECK,		MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_LANGUAGE_FILTER));
	Check_Dlg_Button (IDC_ALLOW_FIND_CHECK,			MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_ALLOW_FIND));
	Check_Dlg_Button (IDC_DISPLAY_ASIAN_CHECK,		MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_DISPLAY_ASIAN));
	Check_Dlg_Button (IDC_DISPLAY_NONASIAN_CHECK,	MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_DISPLAY_NONASIAN));
	Check_Dlg_Button (IDC_SHOW_BUDDIES_CHAT_CHECK,	MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_BUDDY_CHAT_ONLY));
	Check_Dlg_Button (IDC_SHOW_CLAN_CHAT_CHECK,	MPSettingsMgrClass::Get_Option_Flag (MPSettingsMgrClass::OPTION_CLAN_CHAT_ONLY));

	//
	//	Configure the firewall options (port number is 0 by default, no send delay)
	//
	Check_Dlg_Button (IDC_SEND_DELAY_CHECK, false);
	Set_Dlg_Item_Int (IDC_PORT_EDIT, 0);

	MenuDialogClass::On_Init_Dialog ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MultiplayOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	switch (ctrl_id) {
		case IDCANCEL:
		case IDC_MENU_BACK_BUTTON:
			Save_Settings();
			break;

		case IDC_DISPLAY_ASIAN_CHECK: {
			bool latinChecked = Is_Dlg_Button_Checked(IDC_DISPLAY_NONASIAN_CHECK);

			// If filtering asian text then latin text cannot be filtered too.
			if (param == 0 && !latinChecked) {
				Check_Dlg_Button(IDC_DISPLAY_NONASIAN_CHECK, true);
			}
			break;
		}

		case IDC_DISPLAY_NONASIAN_CHECK: {
			bool asianChecked = Is_Dlg_Button_Checked(IDC_DISPLAY_ASIAN_CHECK);

			// If filtering latin text then asian text cannot be filtered too.
			if (param == 0 && !asianChecked) {
				Check_Dlg_Button(IDC_DISPLAY_ASIAN_CHECK, true);
			}
			break;
		}
	}

	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Save_Settings
//
////////////////////////////////////////////////////////////////
void
MultiplayOptionsMenuClass::Save_Settings (void)
{
	cUserOptions::ShowNamesOnSoldier.Set (((CheckBoxCtrlClass *)Get_Dlg_Item (IDC_DISPLAY_PLAYER_NAMES_CHECK))->Get_Check ());

	//
	//	Configure the multiplayer options
	//
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_ALLOW_PAGES,			Is_Dlg_Button_Checked (IDC_ALLOW_PAGES_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_LANGUAGE_FILTER,		Is_Dlg_Button_Checked (IDC_LANGUAGE_FILTER_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_ALLOW_FIND,			Is_Dlg_Button_Checked (IDC_ALLOW_FIND_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_DISPLAY_ASIAN,		Is_Dlg_Button_Checked (IDC_DISPLAY_ASIAN_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_DISPLAY_NONASIAN,	Is_Dlg_Button_Checked (IDC_DISPLAY_NONASIAN_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_BUDDY_CHAT_ONLY,	Is_Dlg_Button_Checked (IDC_SHOW_BUDDIES_CHAT_CHECK));
	MPSettingsMgrClass::Set_Option_Flag (MPSettingsMgrClass::OPTION_CLAN_CHAT_ONLY,	Is_Dlg_Button_Checked (IDC_SHOW_CLAN_CHAT_CHECK));
	return ;
}
