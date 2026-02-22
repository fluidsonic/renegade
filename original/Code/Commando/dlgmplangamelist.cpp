#include "dlgmplangamelist.h"
#include "win.h"
#include "listctrl.h"
#include "gamedata.h"
#include "gameinitmgr.h"
#include "dialogmgr.h"
#include "cnetwork.h"
#include "ww3d.h"
#include "dialogresource.h"
#include "renegadedialogmgr.h"
#include "translatedb.h"
#include "string_ids.h"
#include "dialogcontrol.h"
#include "specialbuilds.h"
#include "editctrl.h"
#include "dlgpasswordprompt.h"
#include "registry.h"
#include "_globals.h"
#include "dlgmplanhostoptions.h"

bool MPLanGameListMenuClass::UpdateNickname = false;

////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
static enum
{
	COL_ICON				= 0,
	COL_HOST_NAME,
	COL_GAME_NAME,
	COL_GAME_MAP,
	COL_PLAYERS,
	COL_MAX
};

////////////////////////////////////////////////////////////////
//	Static member initialization
////////////////////////////////////////////////////////////////
MPLanGameListMenuClass *	MPLanGameListMenuClass::_TheInstance	= NULL;

////////////////////////////////////////////////////////////////
//
//	MPLanGameListMenuClass
//
////////////////////////////////////////////////////////////////
MPLanGameListMenuClass::MPLanGameListMenuClass (void)	:
	UpdateTimer (0),
	MenuDialogClass (IDD_MP_LAN_GAME_LIST)
{
}

MPLanGameListMenuClass::~MPLanGameListMenuClass()
{
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_Init_Dialog (void)
{
/*
#ifdef BETACLIENT
	Get_Dlg_Item(IDC_MENU_MP_LAN_HOST_BUTTON)->Enable(false);
#endif // BETACLIENT
*/

#ifdef FREEDEDICATEDSERVER
	Get_Dlg_Item(IDC_JOIN_GAME_BUTTON)->Enable(false);
#endif // FREEDEDICATEDSERVER

	//
	//	Get a pointer to the list control
	//
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_GAME_LIST_CTRL);
	if (list_ctrl != NULL) {

		WideStringClass col_name;

		//
		//	Configure the columns
		//
		col_name = TRANSLATE (IDS_MP_GAME_LIST_HEADER_ICON);
		list_ctrl->Add_Column (col_name, 0.1F, Vector3 (1, 1, 1));

		col_name = TRANSLATE(IDS_MP_GAME_LIST_HEADER_HOST_NAME);
		list_ctrl->Add_Column (col_name, 0.175F, Vector3 (1, 1, 1));

		col_name = TRANSLATE (IDS_MP_GAME_LIST_HEADER_GAME_NAME);
		list_ctrl->Add_Column (col_name, 0.325F, Vector3 (1, 1, 1));

		col_name = TRANSLATE (IDS_MP_GAME_LIST_HEADER_GAME_MAP);
		list_ctrl->Add_Column (col_name, 0.2F, Vector3 (1, 1, 1));

		col_name = TRANSLATE (IDS_MP_GAME_LIST_HEADER_PLAYERS);
		list_ctrl->Add_Column (col_name, 0.1F, Vector3 (1, 1, 1));

		col_name = TRANSLATE (IDS_MP_GAME_LIST_HEADER_PING);
		list_ctrl->Add_Column (col_name, 0.1F, Vector3 (1, 1, 1));

		//
		//	Refresh the game list in one second
		//
		UpdateTimer = 500;
	}

	//
	//	Put the nickname into the nickname edit control
	//
	EditCtrlClass* nameEdit = (EditCtrlClass*)Get_Dlg_Item(IDC_NICKNAME_EDIT);
	assert(nameEdit != NULL);

	if (nameEdit) {
		//nameEdit->Set_Text_Limit(32);
		nameEdit->Set_Text_Limit(9);
		nameEdit->Set_Text(cNetInterface::Get_Nickname());

		bool enable = (nameEdit->Get_Text_Length() > 0);
		Enable_Dlg_Item(IDC_JOIN_GAME_BUTTON, enable);
		Enable_Dlg_Item(IDC_MENU_MP_LAN_HOST_BUTTON, enable);
	}

	MenuDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Destroy
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_Destroy (void)
{
	//
	//	Set the new nickname
	//
	WideStringClass nickname = Get_Dlg_Item_Text (IDC_NICKNAME_EDIT);
	cNetInterface::Set_Nickname (nickname);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	switch (ctrl_id) {

		case IDC_REFRESH_GAME_LIST:
			Update_Game_List ();
			break;

		case IDC_JOIN_GAME_BUTTON:
			Join_Game ();
			break;

		case IDC_MENU_MP_LAN_HOST_BUTTON:
		{
			//
			//	Set the new nickname
			//
			WideStringClass nickname = Get_Dlg_Item_Text (IDC_NICKNAME_EDIT);
			cNetInterface::Set_Nickname (nickname);

			/*
			//
			//	Cache this value, don't know if we need to or not anymore...
			//
			RegistryClass registry (APPLICATION_SUB_KEY_NAME_MP_SETTINGS);
			if (registry.Is_Valid ()) {
				registry.Set_Int (REG_VALUE_LAST_GAME_TYPE, 0);
			}
			*/

			//
			//	Create the new game data
			//
			if ( PTheGameData != NULL ) {
				delete PTheGameData;
				PTheGameData = NULL;
			}
			PTheGameData = cGameData::Create_Game_Of_Type (cGameData::GAME_TYPE_CNC);

			// LAN games are NEVER quickmatch
			The_Game()->Set_QuickMatch_Server(false);

			The_Game()->Load_From_Server_Config ();

			//
			//	Go to the host game options menu
			//
			START_DIALOG (MPLanHostOptionsMenuClass);
			break;
		}
	}

	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Frame_Update
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_Frame_Update (void)
{
	//
	//	Is it time to update the list?
	//
	UpdateTimer -= WW3D::Get_Frame_Time ();
	if (UpdateTimer <= 0) {

		//
		//	Update the list and reset the timer
		//
		Update_Game_List ();

		UpdateTimer = 20000;
	}

	if (UpdateNickname) {
		EditCtrlClass * nameEdit = (EditCtrlClass*)Get_Dlg_Item(IDC_NICKNAME_EDIT);
		nameEdit->Set_Text(cNetInterface::Get_Nickname());
		UpdateNickname = false;
	}

	MenuDialogClass::On_Frame_Update ();
	return ;
}

bool MPLanGameListMenuClass::On_Key_Down(uint32_t key_id, uint32_t key_data)
{
	if (VK_F5 == key_id) {
		Update_Game_List();
		return true;
	}

	return MenuDialogClass::On_Key_Down(key_id, key_data);
}

////////////////////////////////////////////////////////////////
//
//	Update_Game_List
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::Update_Game_List (void)
{}

////////////////////////////////////////////////////////////////
//
//	Join_Game
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::Join_Game (void)
{}

////////////////////////////////////////////////////////////////
//
// Handle receipt of password entered signal from the password
// prompt dialog.
//
////////////////////////////////////////////////////////////////
void MPLanGameListMenuClass::ReceiveSignal(DlgPasswordPrompt& passwordDialog)
{
	PTheGameData->Set_Password(passwordDialog.GetPassword());
	Connect_To_Server();
}

////////////////////////////////////////////////////////////////
//
//	Connect_To_Server
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::Connect_To_Server (void)
{
	//
	//	Set the new nickname
	//
	WideStringClass nickname = Get_Dlg_Item_Text (IDC_NICKNAME_EDIT);
	cNetInterface::Set_Nickname (nickname);

	//
	//	Start the client
	//
	cNetwork::Init_Client ();

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_ListCtrl_Delete_Entry
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_ListCtrl_Delete_Entry
(
	ListCtrlClass *list_ctrl,
	int				ctrl_id,
	int				item_index
)
{}

////////////////////////////////////////////////////////////////
//
//	On_ListCtrl_DblClk
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_ListCtrl_DblClk
(
	ListCtrlClass *list_ctrl,
	int				ctrl_id,
	int				item_index
)
{
	Join_Game ();
	return ;
}

void MPLanGameListMenuClass::On_EditCtrl_Change(EditCtrlClass* edit, int id)
{
	if (IDC_NICKNAME_EDIT == id) {
		// Do not allow leading or trailing whitespace
		WideStringClass text(0, true);
		text = edit->Get_Text();
		text.Trim();
		edit->Set_Text(text);

		bool enable = (edit->Get_Text_Length() > 0);
		Enable_Dlg_Item(IDC_JOIN_GAME_BUTTON, enable);
		Enable_Dlg_Item(IDC_MENU_MP_LAN_HOST_BUTTON, enable);
	}
}

////////////////////////////////////////////////////////////////
//
//	Display
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::Display (void)
{
	//
	//	Create the dialog if necessary, otherwise simply bring it to the front
	//
	if (_TheInstance == NULL) {
		START_DIALOG (MPLanGameListMenuClass);
	} else {
		if (_TheInstance->Is_Active_Menu () == false) {
			DialogMgrClass::Rollback (_TheInstance);
		}
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Last_Menu_Ending
//
////////////////////////////////////////////////////////////////
void
MPLanGameListMenuClass::On_Last_Menu_Ending (void)
{
	RenegadeDialogMgrClass::Goto_Location (RenegadeDialogMgrClass::LOC_MAIN_MENU);
	return ;
}

	/*
	if (UpdateNickname && !g_awaiting_edit) {
		::MessageBeep(MB_OK);//XXX
		EditCtrlClass* nameEdit = (EditCtrlClass*)Get_Dlg_Item(IDC_NICKNAME_EDIT);
		assert(nameEdit != NULL);
		nameEdit->Set_Focus();
		g_awaiting_edit = true;
	}
	*/

		//g_awaiting_edit = false;
		//UpdateNickname = false;
//bool g_awaiting_edit = false;//XXX
//#include "dlgmplangametype.h"
