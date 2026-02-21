#include "dlgcheatoptions.h"
#include "listctrl.h"
#include "cheatmgr.h"

////////////////////////////////////////////////////////////////
//
//	CheatOptionsMenuClass
//
////////////////////////////////////////////////////////////////
CheatOptionsMenuClass::CheatOptionsMenuClass (void)	:
	MenuDialogClass (IDD_OPTIONS_CHEATS)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
CheatOptionsMenuClass::On_Init_Dialog (void)
{
	CheatMgrClass *cheat_mgr = CheatMgrClass::Get_Instance ();

	Check_Dlg_Button (IDC_INVULN_CHECK,			cheat_mgr->Is_Cheat_Set (CheatMgrClass::CHEAT_INVULNERABILITY));
	Check_Dlg_Button (IDC_UNLIM_AMMO_CHECK,	cheat_mgr->Is_Cheat_Set (CheatMgrClass::CHEAT_INFINITE_AMMO));
	Check_Dlg_Button (IDC_GIVE_WEAPONS_CHECK, cheat_mgr->Is_Cheat_Set (CheatMgrClass::CHEAT_ALL_WEAPONS));

	MenuDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
CheatOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Destroy
//
////////////////////////////////////////////////////////////////
void
CheatOptionsMenuClass::On_Destroy (void)
{
	CheatMgrClass *cheat_mgr = CheatMgrClass::Get_Instance ();

	cheat_mgr->Enable_Cheat (CheatMgrClass::CHEAT_INVULNERABILITY, Is_Dlg_Button_Checked (IDC_INVULN_CHECK));
	cheat_mgr->Enable_Cheat (CheatMgrClass::CHEAT_INFINITE_AMMO, Is_Dlg_Button_Checked (IDC_UNLIM_AMMO_CHECK));
	cheat_mgr->Enable_Cheat (CheatMgrClass::CHEAT_ALL_WEAPONS, Is_Dlg_Button_Checked (IDC_GIVE_WEAPONS_CHECK));

	MenuDialogClass::On_Destroy ();
	return ;
}
