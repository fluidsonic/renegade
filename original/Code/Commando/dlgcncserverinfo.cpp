#include "dlgcncserverinfo.h"
#include "gamedata.h"
#include "resource.h"
#include "listctrl.h"
#include "imagectrl.h"
#include "playertype.h"
#include "combat.h"
#include "playermanager.h"
#include "player.h"
#include "soldier.h"
#include "gameinitmgr.h"
#include "gamemode.h"
#include "input.h"
#include "healthbarctrl.h"
#include "basecontroller.h"
#include "building.h"
#include "damage.h"
#include "vehicle.h"
#include "resource.h"
#include "translatedb.h"
#include "string_ids.h"
#include "mousemgr.h"
#include "directinput.h"


////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
enum
{
	COL_OPTION,
	COL_VALUE,
};


////////////////////////////////////////////////////////////////
//
//	CNCServerInfoDialogClass
//
////////////////////////////////////////////////////////////////
CNCServerInfoDialogClass::CNCServerInfoDialogClass (void)	:
	MenuDialogClass (IDD_CNC_SERVER_INFO)
{
	return ;
}


////////////////////////////////////////////////////////////////
//
//	~CNCServerInfoDialogClass
//
////////////////////////////////////////////////////////////////
CNCServerInfoDialogClass::~CNCServerInfoDialogClass (void)
{
	GameInitMgrClass::Continue_Game ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
CNCServerInfoDialogClass::On_Init_Dialog (void)
{
	MenuDialogClass::On_Init_Dialog ();
	MouseMgrClass::Show_Cursor (false);

	//
	//	Get a pointer to the list control
	//
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_LIST_CTRL);
	if (list_ctrl != NULL) {

		//
		//	Configure the columns
		//
		list_ctrl->Add_Column (TRANSLATE (IDS_MENU_SERVER_OPTION),		0.5F,	Vector3 (1, 1, 1));
		list_ctrl->Add_Column (TRANSLATE (IDS_MENU_VALUE),					0.5F,	Vector3 (1, 1, 1));

		//
		//	Get the current description of the game
		//
		WideStringClass description;
		The_Game ()->Get_Description (description);
		const WCHAR *buffer = description.Peek_Buffer ();

		const WCHAR DELIMITER	= L'\t';
		const WCHAR NEWLINE		= L'\n';

		//
		//	Fill the settings into the list control
		//
		WideStringClass string (100, true);
		int list_index		= 0;
		int item_index		= 0;
		int index			= 0;
		while (buffer[index] != 0) {
			
			if (buffer[index] == DELIMITER) {
				
				//
				//	Insert a new entry
				//
				item_index = list_ctrl->Insert_Entry (list_index ++, string);
				string.Get_Buffer (100)[0] = 0;
			} else if (buffer[index] == NEWLINE) {

				//
				//	Fill in the setting value
				//
				item_index = list_ctrl->Set_Entry_Text (item_index, COL_VALUE, string);
				string.Get_Buffer (100)[0] = 0;
			} else {

				//
				//	Simply add this character to the string
				//
				string += buffer[index];
			}

			index ++;
		}
	}

	//
	//	Activate the menu game mode (if necessary)
	//
	GameModeClass *menu_game_mode = GameModeManager::Find ("Menu");
	if (menu_game_mode != NULL && menu_game_mode->Is_Active () == false) {
		menu_game_mode->Activate ();
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	On_Frame_Update
//
////////////////////////////////////////////////////////////////
void
CNCServerInfoDialogClass::On_Frame_Update (void)
{
	//
	//	End the dialog when the user releases the player list key
	//
	int dik_id = Input::Get_Primary_Key_For_Function (INPUT_FUNCTION_SERVER_INFO_TOGGLE);
	if ((DirectInput::Get_Keyboard_Button (dik_id) & DirectInput::DI_BUTTON_HELD) == 0) {
		End_Dialog ();
	}

	return ;
}


