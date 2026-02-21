#include "dlginternetoptions.h"
#include "string_ids.h"
#include "dialogresource.h"
#include "shortcutbarctrl.h"
#include "translatedb.h"


////////////////////////////////////////////////////////////////
//
//	InternetOptionsMenuClass
//
////////////////////////////////////////////////////////////////
InternetOptionsMenuClass::InternetOptionsMenuClass (void)	:
	MenuDialogClass (IDD_OPTIONS_INTERNET)
{
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
InternetOptionsMenuClass::On_Init_Dialog (void)
{
	WideStringClass entry_text;

	//
	//	Configure the shortcut bar
	//
	ShortcutBarCtrlClass *bar = (ShortcutBarCtrlClass *)Get_Dlg_Item (IDC_SHORTCUT_BAR);
	if (bar != NULL) {

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_BUDDIES);
		bar->Add_Button (IDC_MP_SHORTCUT_BUDDIES, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_CHAT);
		bar->Add_Button (IDC_MP_SHORTCUT_CHAT, entry_text);

		entry_text = TRANSLATE (IDS_MENU_TEXT364);
		bar->Add_Button (IDC_MP_SHORTCUT_QUICKMATCH_OPTIONS, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_NEWS);
		bar->Add_Button (IDC_MP_SHORTCUT_NEWS, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_INSIDER);
		bar->Add_Button (IDC_MP_SHORTCUT_INSIDER, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_CLANS);
		bar->Add_Button (IDC_MP_SHORTCUT_CLANS, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_RANKINGS);
		bar->Add_Button (IDC_MP_SHORTCUT_RANKINGS, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_ACCOUNT);
		bar->Add_Button (IDC_MP_SHORTCUT_ACCOUNT, entry_text);

		entry_text = TRANSLATE (IDS_MP_SHORTCUT_NET_STATUS);
		bar->Add_Button (IDC_MP_SHORTCUT_NET_STATUS, entry_text);		
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
InternetOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

