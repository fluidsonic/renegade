#include "global.h"
#include "dlgmplanhostoptions.h"
#include "tabctrl.h"
#include "gamedata.h"
#include "netutil.h"
#include "listctrl.h"
#include "comboboxctrl.h"
#include "gameinitmgr.h"
#include "renegadedialogmgr.h"
#include "gdcnc.h"
#include "devoptions.h"
#include "translatedb.h"
#include "string_ids.h"
#include "translatedb.h"
#include "nicenum.h"
#include "useroptions.h"
#include "gamemode.h"
#include "campaign.h"
#include "mpsettingsmgr.h"
#include "dlgserversaveload.h"
#include "modpackagemgr.h"
#include "modpackage.h"


////////////////////////////////////////////////////////////////
//
//	MPLanHostOptionsMenuClass
//
////////////////////////////////////////////////////////////////
MPLanHostOptionsMenuClass::MPLanHostOptionsMenuClass (void)	:
	MenuDialogClass (IDD_MP_LAN_HOST_OPTIONS),
	mStartTheGame (false),
	MapCycleDialog (NULL)
{
	return ;
}

MPLanHostOptionsMenuClass::~MPLanHostOptionsMenuClass (void)
{

	REF_PTR_RELEASE (MapCycleDialog);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostOptionsMenuClass::On_Init_Dialog (void)
{

	Set_Dlg_Item_Text(IDC_GAME_TYPE_TITLE, TRANSLATE(IDS_MP_GAME_CNC));

	TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_TABCTRL);
	if (tab_ctrl != NULL) {

		//
		//	Add the tabs to the control
		//
		TABCTRL_ADD_TAB (tab_ctrl, MPLanHostBasicOptionsTabClass);

		//
		//	Add any mode-specific tabs to the control
		//
		if (The_Game ()->As_Cnc () != NULL) {
			TABCTRL_ADD_TAB (tab_ctrl, MPLanHostCnCOptionsTabClass);
		}

		TABCTRL_ADD_TAB (tab_ctrl, MPLanHostAdvancedOptionsTabClass);
		TABCTRL_ADD_TAB (tab_ctrl, MPLanHostVictoryOptionsTabClass);

		//
		//	Keep a pointer around to the map cycle tab so we can
		// modify its contents as necessary
		//
		MapCycleDialog = new MPLanHostMapCycleOptionsTabClass;
		tab_ctrl->Add_Tab (MapCycleDialog);
	}

	MenuDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Enable_Mod_Selection
//
////////////////////////////////////////////////////////////////
void
MPLanHostOptionsMenuClass::Enable_Mod_Selection (bool onoff)
{
	if (MapCycleDialog == NULL) {
		return ;
	}

	MapCycleDialog->Enable_Mod_Selection (onoff);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Periodic
//
////////////////////////////////////////////////////////////////
void
MPLanHostOptionsMenuClass::On_Periodic (void)
{
	MenuDialogClass::On_Periodic();

	if (mStartTheGame) {

		Start_Game(The_Game());
		mStartTheGame = false;

		End_Dialog();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MPLanHostOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	switch (ctrl_id) {
		case IDC_MENU_MP_LAN_START_BUTTON:
			{
				//
				//	Get a pointer to the tab ctrl
				//
				TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_TABCTRL);
				if (tab_ctrl != NULL) {

					//
					//	Save the changes on each tab of the control
					//
					if (tab_ctrl->Apply_Changes_On_Tabs ()) {

						//
						//	Are the settings valid?
						//
						WideStringClass outMsg;

						if (The_Game()->Is_Valid_Settings (outMsg, true)) {

							//
							//	Save the settings
							//
							The_Game()->Save_To_Server_Config ();

							if (cUserOptions::Get_Bandwidth_Type() == BANDWIDTH_AUTO) {
								if (The_Game()->Get_Max_Players() > 2) {
									int available_bw = cUserOptions::BandwidthBps.Get();
									int required_bw = The_Game()->Get_Max_Players() * 64000;
									int set_bw = min(required_bw, available_bw);
									cUserOptions::BandwidthBps.Set(set_bw);
								}
							}

							Start_Game(The_Game());
						} else {
							WideStringClass errorMsg(0, true);
							errorMsg.Format(u"%s\n\n%s", TRANSLATE(IDS_MENU_TEXT330), (const WCHAR*)outMsg);
							DlgMsgBox::DoDialog(TRANSLATE(IDS_MENU_TEXT329), errorMsg);
						}
					}
				}
			}
			break;

		case IDC_SAVELOAD_BUTTON:
			TabCtrlClass *tab_ctrl = (TabCtrlClass *)Get_Dlg_Item (IDC_TABCTRL);
			if (tab_ctrl != NULL) {

				//
				//	Save the changes on each tab of the control back to the game data.
				//
				if (tab_ctrl->Apply_Changes_On_Tabs()) {
					End_Dialog();
					START_DIALOG(ServerSaveLoadMenuClass);
				}
			}
			break;
	}

	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

void MPLanHostOptionsMenuClass::Start_Game(cGameData* theGame)
{
	CampaignManager::Select_Backdrop_Number_By_MP_Type( theGame->Get_Game_Type() );

	GameInitMgrClass::Set_Is_Client_Required(theGame->IsDedicated.Is_False());
	GameInitMgrClass::Set_Is_Server_Required(true);

	int side = cNetInterface::Get_Side_Preference();
	GameInitMgrClass::Start_Game(theGame->Get_Map_Name(), side);
}

////////////////////////////////////////////////////////////////
//
//	MPLanHostBasicOptionsTabClass
//
////////////////////////////////////////////////////////////////
MPLanHostBasicOptionsTabClass* MPLanHostBasicOptionsTabClass::_mInstance = NULL;
int MPLanHostBasicOptionsTabClass::BandTestMaxPlayers = 0;

MPLanHostBasicOptionsTabClass* MPLanHostBasicOptionsTabClass::Get_Instance(void)
{
	if (_mInstance) {
		_mInstance->Add_Ref();
		return _mInstance;
	}

	return NULL;
}

MPLanHostBasicOptionsTabClass::MPLanHostBasicOptionsTabClass (void)	:
	ChildDialogClass (IDD_MP_LAN_HOST_OPTIONS_BASIC_TAB)
{
	assert(_mInstance == NULL);
	_mInstance = this;
}

MPLanHostBasicOptionsTabClass::~MPLanHostBasicOptionsTabClass (void)
{
	_mInstance = NULL;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostBasicOptionsTabClass::On_Init_Dialog (void)
{
	//
	//	Fill in the game name
	//
	((EditCtrlClass *)Get_Dlg_Item (IDC_GAME_NAME_EDIT))->Set_Text_Limit (25);
	Set_Dlg_Item_Text (IDC_GAME_NAME_EDIT, The_Game ()->Get_Game_Title ());

	//
	//	Fill in the password control on the dialog
	//
	((EditCtrlClass *)Get_Dlg_Item (IDC_PASSWORD_EDIT))->Set_Text_Limit (15);
	//Set_Dlg_Item_Text (IDC_PASSWORD_EDIT, The_Game ()->Get_Password ());

	/*
	if (cGameSpyAdmin::Is_Gamespy_Game()) {
		Get_Dlg_Item(IDC_PASSWORD_EDIT)->Enable(false);
	}
	*/

	//
	//	Fill in the max-players control on the dialog
	//
	Set_Dlg_Item_Int (IDC_NUM_PLAYERS_EDIT, min(The_Game ()->Get_Max_Players (), NetworkObjectClass::MAX_CLIENT_COUNT-1));

	//
	//	Configure the IP NIC Enumeration combobox
	//
	ComboBoxCtrlClass *nic_combobox = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_HOSTING_IP_COMBO);

	if (nic_combobox != NULL) {

		uint32_t * nics = cNicEnum::Get_Nics();
		int nic_count = cNicEnum::Get_Num_Nics();
		uint32_t preferred_nick = cUserOptions::PreferredLanNic.Get();

		int current_index = -1;

		for (USHORT index = 0; index < nic_count; index++) {
			WideStringClass nic_string;
			nic_string.Convert_From(cNetUtil::Address_To_String(nics[index]));
			nic_combobox->Add_String( nic_string );

			if (nics[index] == preferred_nick) {
					current_index = index;
			}
		}

		//
		//	Select the default entry in the radar_combobox
		//
		nic_combobox->Set_Curr_Sel ( current_index );

		Enable_Dlg_Item(IDC_HOSTING_IP_COMBO, true);
	}

	int sidePref = cNetInterface::Get_Side_Preference();

	InitSideChoiceCombo(sidePref);

	ChildDialogClass::On_Init_Dialog ();
	return ;
}

/******************************************************************************
*
* NAME
*     MPLanHostBasicOptionsTabClass::InitSideChoiceCombo
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void MPLanHostBasicOptionsTabClass::InitSideChoiceCombo(int sidePref)
{
	ComboBoxCtrlClass* combo = (ComboBoxCtrlClass*)Get_Dlg_Item(IDC_CHOOSESIDE_COMBO);

	if (combo) {
		//(gth) Renegade day 120 Patch: re-translate these strings each time!
		struct {const char16_t* TeamName; int TeamID;} _teams[] = {
			{TRANSLATE (IDS_MENU_AUTO_TEAM), PLAYERTYPE_RENEGADE},
			{TRANSLATE (IDS_MENU_TEXT933), PLAYERTYPE_GDI},
			{TRANSLATE (IDS_MENU_TEXT934), PLAYERTYPE_NOD},
			{NULL, -1},
		};

		int index = 0;

		while (_teams[index].TeamName != NULL) {
			int item = combo->Add_String(_teams[index].TeamName);

			if (item >= 0) {
				combo->Set_Item_Data(item, (uint32_t)_teams[index].TeamID);
			}

			if (_teams[index].TeamID == sidePref) {
				combo->Set_Curr_Sel(index);
			}

			++index;
		}

		if (combo->Get_Curr_Sel() == -1) {
			combo->Set_Curr_Sel(0);
		}
	}
}

////////////////////////////////////////////////////////////////
//
//	On_Apply
//
////////////////////////////////////////////////////////////////
bool
MPLanHostBasicOptionsTabClass::On_Apply (void)
{
	WideStringClass password = Get_Dlg_Item_Text (IDC_PASSWORD_EDIT);
	The_Game ()->IsPassworded.Set ((password.Get_Length () > 0));

	//
	//	Pass our settings onto the game
	//
	The_Game ()->Set_Game_Title (Get_Dlg_Item_Text (IDC_GAME_NAME_EDIT));
	The_Game ()->Set_Password (password);
	// Has to be -1 since we use the last client as a reference for refreshing dirty bits.
	The_Game ()->Set_Max_Players (min(Get_Dlg_Item_Int (IDC_NUM_PLAYERS_EDIT), NetworkObjectClass::MAX_CLIENT_COUNT - 1));

	// Quickmatch games can not have passwords
	if (The_Game()->IsPassworded.Is_True()) {
		The_Game()->Set_QuickMatch_Server(false);
	}

	//
	//	Read the IP NIC Enumeration combobox
	//
	{
		ComboBoxCtrlClass *nic_combobox = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_HOSTING_IP_COMBO);
		if (nic_combobox != NULL) {
			int curr_sel = nic_combobox->Get_Curr_Sel ();
			if (curr_sel >= 0) {
				uint32_t * nics = cNicEnum::Get_Nics();
				cUserOptions::PreferredLanNic.Set(nics[curr_sel]);
				The_Game()->Set_Ip_Address(nics[curr_sel]);
			}
		}
	}

	ComboBoxCtrlClass* combo = (ComboBoxCtrlClass*)Get_Dlg_Item(IDC_CHOOSESIDE_COMBO);

	if (combo) {
		int curSel = combo->Get_Curr_Sel();
		int side = combo->Get_Item_Data(curSel);
		cNetInterface::Set_Side_Preference(side);
	}

	return true;
}

////////////////////////////////////////////////////////////////
//
//	On_EditCtrl_Change
//
////////////////////////////////////////////////////////////////
void
MPLanHostBasicOptionsTabClass::On_EditCtrl_Change (EditCtrlClass *edit, int ctrlID)
{
	if (IDC_PASSWORD_EDIT == ctrlID) {

		//
		// Flag the game as passworded if the user enters text into the password edit.
		//
		const WCHAR* text = edit->Get_Text ();
		bool hasPassword = (text && (wcslen (text) > 0));
		SendSignal (hasPassword);

	} else if (ctrlID == IDC_NUM_PLAYERS_EDIT) {

		//
		//	Check to ensure the player count is within bounds...
		//
		int player_count = Get_Dlg_Item_Int (IDC_NUM_PLAYERS_EDIT);
		if (player_count < 0 || player_count > NetworkObjectClass::MAX_CLIENT_COUNT-1) {
			player_count = min (player_count, NetworkObjectClass::MAX_CLIENT_COUNT-1);
			player_count = max (player_count, 0);
			Set_Dlg_Item_Int (IDC_NUM_PLAYERS_EDIT, player_count);
		}
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	MPLanHostAdvancedOptionsTabClass
//
////////////////////////////////////////////////////////////////
MPLanHostAdvancedOptionsTabClass::MPLanHostAdvancedOptionsTabClass (void)	:
	ChildDialogClass (IDD_MP_LAN_HOST_OPTIONS_ADVANCED_TAB)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostAdvancedOptionsTabClass::On_Init_Dialog (void)
{
	MPLanHostBasicOptionsTabClass* basicTab = MPLanHostBasicOptionsTabClass::Get_Instance();

	if (basicTab) {
		basicTab->SignalMe(*this);
		basicTab->Release_Ref();
	}

// FDS: host options used command-line args instead of UI

//	if (cGameSpyAdmin::Is_Gamespy_Game()) {
//		assert(PTheGameData != NULL);
//		The_Game ()->IsDedicated.Set(true);
//		Enable_Dlg_Item (IDC_DEDICATED_SERVER_CHECK,		false);
//	}

	Check_Dlg_Button (IDC_DEDICATED_SERVER_CHECK,	The_Game ()->IsDedicated.Is_True ());

	// Change teams is ON if team editing and changing team allowed is true
	bool canChangeTeams = The_Game()->Is_Editable_Teaming();
	mChangeTeams = canChangeTeams && The_Game()->IsTeamChangingAllowed.Is_True();
	Check_Dlg_Button(IDC_TEAM_CHANGE_CHECK, mChangeTeams);
	Enable_Dlg_Item(IDC_TEAM_CHANGE_CHECK, canChangeTeams);

	// Remix teams is ON if team change is FALSE and remix setting is true.
	mRemixTeams = The_Game()->IsTeamChangingAllowed.Is_False() && The_Game()->RemixTeams.Is_True();
	Check_Dlg_Button(IDC_REMIX_TEAMS_CHECK, mRemixTeams);
	Enable_Dlg_Item(IDC_REMIX_TEAMS_CHECK, The_Game()->IsTeamChangingAllowed.Is_False());

	//Check_Dlg_Button (IDC_TRUST_CLIENTS_CHECK,		The_Game ()->IsClientTrusted.Is_True ());

	mPassword = The_Game()->IsPassworded.Get();

	if (The_Game()->IsDedicated.Is_False()) {
		Check_Dlg_Button(IDC_SERVER_RESTART_CHECK, false);
		Enable_Dlg_Item(IDC_SERVER_RESTART_CHECK, false);
	}

	//
	//	Fill in the Motd control
	//
	Set_Dlg_Item_Text (IDC_MESSAGE_OF_THE_DAY_EDIT, The_Game ()->Get_Motd ());

	ChildDialogClass::On_Init_Dialog ();

	//
	//	Don't allow the user to select a MOD package if the game is laddered
	//
	MPLanHostOptionsMenuClass *parent_dlg = static_cast<MPLanHostOptionsMenuClass *> (Get_Parent_Dialog ());
	if (parent_dlg != NULL) {
		parent_dlg->Enable_Mod_Selection (true);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Apply
//
////////////////////////////////////////////////////////////////
bool
MPLanHostAdvancedOptionsTabClass::On_Apply (void)
{
	//
	//	Pass our settings onto the game
	//
	The_Game()->IsDedicated.Set (Is_Dlg_Button_Checked (IDC_DEDICATED_SERVER_CHECK));
	The_Game()->IsTeamChangingAllowed.Set (Is_Dlg_Button_Checked (IDC_TEAM_CHANGE_CHECK));
	The_Game()->Set_QuickMatch_Server(Is_Dlg_Button_Checked(IDC_ALLOW_QUICKMATCH));
	The_Game()->IsLaddered.Set (Is_Dlg_Button_Checked (IDC_LADDERED_CHECK));
	The_Game()->IsClanGame.Set (Is_Dlg_Button_Checked (IDC_CLAN_GAME_CHECK));
	//The_Game()->IsClientTrusted.Set (Is_Dlg_Button_Checked (IDC_TRUST_CLIENTS_CHECK));
	The_Game()->RemixTeams.Set (Is_Dlg_Button_Checked (IDC_REMIX_TEAMS_CHECK));

	//
	// TSS022402
	// If laddering is on, silently turn off FF. Exploit concerns.
	// N.B. This is a hack. Leverages off the fact that
	// MPLanHostCnCOptionsTabClass::On_Apply is called first.
	//
	if (The_Game()->IsLaddered.Is_True()) {
		The_Game()->IsFriendlyFirePermitted.Set(false);
	}

	//
	// Save the message of the day (MOTD)
	//
	WideStringClass motd = Get_Dlg_Item_Text (IDC_MESSAGE_OF_THE_DAY_EDIT);
	if (motd.Get_Length() > MAX_MOTD_LENGTH) {
		motd.Peek_Buffer()[MAX_MOTD_LENGTH - 1] = 0;
	}
	The_Game ()->Set_Motd (motd);

	return true;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MPLanHostAdvancedOptionsTabClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	bool restart_enabled = true;

	switch (ctrl_id) {
		case IDC_DEDICATED_SERVER_CHECK: {
			// Enable the restart button only if we have auto login
			if (Is_Dlg_Button_Checked(IDC_DEDICATED_SERVER_CHECK)) {
				restart_enabled = true;
			} else {
				restart_enabled = false;
			}

			Enable_Dlg_Item(IDC_CLAN_GAME_CHECK, false);
			Check_Dlg_Button(IDC_CLAN_GAME_CHECK, false);
		}
		break;

		case IDC_SERVER_RESTART_CHECK:
		break;

		case IDC_TEAM_CHANGE_CHECK:
			{
			mChangeTeams = (param != 0);

			// Change Team and Clans are mutually exclusive
			Enable_Dlg_Item(IDC_CLAN_GAME_CHECK, false);
			Check_Dlg_Button(IDC_CLAN_GAME_CHECK, false);

			// Change Team and laddered are mutually exclusive
			Enable_Dlg_Item(IDC_LADDERED_CHECK, !mChangeTeams);
			Check_Dlg_Button(IDC_LADDERED_CHECK, false);
			}
			break;

		case IDC_REMIX_TEAMS_CHECK:
			{
			mRemixTeams = (param != 0);

			// Remix teams and clans are mutually exclusive
			Enable_Dlg_Item(IDC_CLAN_GAME_CHECK, false);
			Check_Dlg_Button(IDC_CLAN_GAME_CHECK, false);
			}
			break;

		// If clan is checked then quickmatch is not allowed.
		case IDC_CLAN_GAME_CHECK:
			break;

		// If quickmatch is checked then laddered must be checked as well.
		case IDC_ALLOW_QUICKMATCH:
			break;

		// If clan is checked then quickmatch is not allowed.
		case IDC_LADDERED_CHECK:
			break;
	}
}

void MPLanHostAdvancedOptionsTabClass::HandleNotification (DlgMsgBoxEvent &event)
{
	//
	// We only use a message box for the auto login confirm.
	//
	if (event.Event() == DlgMsgBoxEvent::Yes) {
		//Enable_Dlg_Item (IDC_SERVER_RESTART_CHECK, true);
		Check_Dlg_Button(IDC_SERVER_RESTART_CHECK, true);
		const char* login = MPSettingsMgrClass::Get_Last_Login();
		MPSettingsMgrClass::Set_Auto_Login(login);
	}
}

void MPLanHostAdvancedOptionsTabClass::ReceiveSignal(bool& hasPassword)
{
	if (mPassword != hasPassword) {
		mPassword = hasPassword;
	}
}

////////////////////////////////////////////////////////////////
//
//	MPLanHostMapCycleOptionsTabClass
//
////////////////////////////////////////////////////////////////
MPLanHostMapCycleOptionsTabClass::MPLanHostMapCycleOptionsTabClass (void)	:
	ChildDialogClass (IDD_MP_LAN_HOST_OPTIONS_MAP_TAB)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	List_Contains
//
////////////////////////////////////////////////////////////////
bool
List_Contains(DynamicVectorClass<WideStringClass> & list, WideStringClass & item)
{
	for (int index = 0; index < list.Count (); index ++) {
		if (list[index] == item) {
			return true;
		}
	}

	return false;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::On_Init_Dialog (void)
{
	Build_Mod_Package_List ();

	//
	//	Create the available maps list
	//
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_AVAILABLE_MAP_LIST_CTRL);
	if (list_ctrl != NULL) {
		list_ctrl->Add_Column (u"", 1.0F, Vector3 (1, 1, 1));
	}

	//
	//	Create the map cycle list
	//
	list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_MAP_CYCLE_LIST_CTRL);
	if (list_ctrl != NULL) {
		list_ctrl->Add_Column (u"", 1.0F, Vector3 (1, 1, 1));
	}

	//
	//	Build the map lists
	//
	Populate_Map_List_Ctrl ();

	//
	//	Fill in the map time limit
	//
	((EditCtrlClass *)Get_Dlg_Item (IDC_MAP_TIME_LIMIT_EDIT))->Set_Text_Limit (3);
	Set_Dlg_Item_Int (IDC_MAP_TIME_LIMIT_EDIT, The_Game ()->Get_Time_Limit_Minutes ());
	Check_Dlg_Button (IDC_LOOP_MAPS_CHECK,	The_Game ()->Do_Maps_Loop ());

	ChildDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Apply
//
////////////////////////////////////////////////////////////////
bool
MPLanHostMapCycleOptionsTabClass::On_Apply (void)
{
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_MAP_CYCLE_LIST_CTRL);
	if (list_ctrl != NULL) {
		StringClass ascii_string;

		//
		//	Convert the map name from wide-format to ascii-format
		//
		if (list_ctrl->Get_Entry_Count () > 0) {
			WideStringClass map_name (list_ctrl->Get_Entry_Text (0, 0));
			map_name.Convert_To (ascii_string);
		}

		//
		//	Set the map name
		//
		The_Game ()->Set_Map_Name (ascii_string);

		//
		//	Configure the radar mode combobox
		//
		ComboBoxCtrlClass *combobox_ctrl = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_MOD_PACKAGE_COMBO);
		if (combobox_ctrl != NULL) {

			//
			//	Lookup the currently selected mod package
			//
			int curr_sel = combobox_ctrl->Get_Curr_Sel ();
			if (curr_sel != -1) {
				const ModPackageClass *package = (const ModPackageClass *)combobox_ctrl->Get_Item_Data (curr_sel);
				if (package != NULL) {
					The_Game ()->Set_Mod_Name (package->Get_Package_Filename ());
				} else {
					The_Game ()->Set_Mod_Name ("");
				}
			} else {
				The_Game ()->Set_Mod_Name ("");
			}
		}

		//
		// Save the map cycle information
		//
		The_Game()->Clear_Map_Cycle();
		for (int i = 0; i < list_ctrl->Get_Entry_Count(); i++) {
			if (i < cGameData::MAX_MAPS) {
				WideStringClass map_name (list_ctrl->Get_Entry_Text (i, 0));
				map_name.Convert_To (ascii_string);
				The_Game()->Set_Map_Cycle(i, ascii_string);
			}
		}
	}

	//
	//	Save the map time limit
	//
	The_Game ()->Set_Time_Limit_Minutes (Get_Dlg_Item_Int (IDC_MAP_TIME_LIMIT_EDIT));
	The_Game ()->Set_Do_Maps_Loop (Is_Dlg_Button_Checked (IDC_LOOP_MAPS_CHECK));
	return true;
}

////////////////////////////////////////////////////////////////
//
//	Enable_Mod_Selection
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Enable_Mod_Selection (bool onoff)
{
	ComboBoxCtrlClass *combobox_ctrl = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_MOD_PACKAGE_COMBO);
	if (combobox_ctrl == NULL) {
		return ;
	}

	//
	//	Either enable or disable the mod selector as necessary
	//
	if (onoff) {
		Enable_Dlg_Item (IDC_MOD_PACKAGE_COMBO, true);
	} else {
		Enable_Dlg_Item (IDC_MOD_PACKAGE_COMBO, false);

		//
		//	Ensure no mod package is selected and rebuild the map list if necessary
		//
		if (combobox_ctrl->Get_Curr_Sel () != 0) {
			combobox_ctrl->Set_Curr_Sel (0);
			Populate_Map_List_Ctrl ();
		}
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Build_Mod_Package_List
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Build_Mod_Package_List (void)
{
	//
	//	Configure the radar mode combobox
	//
	ComboBoxCtrlClass *combobx_ctrl = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_MOD_PACKAGE_COMBO);
	if (combobx_ctrl == NULL) {
		return ;
	}

	ModPackageMgrClass::Reset_List ();
	ModPackageMgrClass::Build_List ();

	//
	//	Get the name of the default mod package
	//
	StringClass ansi_default_pkg = ModPackageMgrClass::Get_Current_Package ().Get_Name ();
	WideStringClass default_pkg;
	default_pkg.Convert_From (ansi_default_pkg);

	//
	//	Add a default entry to the list
	//
	combobx_ctrl->Add_String (u"<None>");

	//
	//	Loop over and add all the mod packages to the combobox
	//
	bool found	= false;
	int count	= ModPackageMgrClass::Get_Package_Count ();
	for (int index = 0; index < count; index ++) {
		const ModPackageClass *package = ModPackageMgrClass::Get_Package (index);
		if (package != NULL) {

			//
			//	Add an entry for this mod package to the combobox
			//
			WideStringClass curr_name;
			curr_name.Convert_From (package->Get_Name ());

			int item_index = combobx_ctrl->Add_String (curr_name);
			if (item_index != -1) {
				combobx_ctrl->Set_Item_Data (item_index, (uint32_t)(uintptr_t)package);

				//
				//	Is this the default entry?  If so select it...
				//
				if (default_pkg.Compare_No_Case (curr_name) == 0) {
					found = true;
					combobx_ctrl->Set_Curr_Sel (item_index);
				}
			}
		}
	}

	//
	//	Select the first entry by default
	//
	if (found == false) {
		combobx_ctrl->Set_Curr_Sel (0);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Add_Map
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Add_Map (void)
{
	ListCtrlClass *avail_list = (ListCtrlClass *)Get_Dlg_Item (IDC_AVAILABLE_MAP_LIST_CTRL);
	ListCtrlClass *cycle_list = (ListCtrlClass *)Get_Dlg_Item (IDC_MAP_CYCLE_LIST_CTRL);

	//
	//	Get the current selection, and add it to the cycle list
	//
	int curr_sel = avail_list->Get_Curr_Sel ();
	if (curr_sel >= 0) {
		cycle_list->Insert_Entry (0xFF, avail_list->Get_Entry_Text (curr_sel, 0));
		avail_list->Delete_Entry (curr_sel);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Remove_Map
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Remove_Map (void)
{
	ListCtrlClass *avail_list = (ListCtrlClass *)Get_Dlg_Item (IDC_AVAILABLE_MAP_LIST_CTRL);
	ListCtrlClass *cycle_list = (ListCtrlClass *)Get_Dlg_Item (IDC_MAP_CYCLE_LIST_CTRL);

	//
	//	Get the current selection
	//
	int curr_sel = cycle_list->Get_Curr_Sel ();
	if (curr_sel >= 0) {

		//
		//	Remove this entry from the list
		//
		avail_list->Insert_Entry (0xFF, cycle_list->Get_Entry_Text (curr_sel, 0));
		cycle_list->Delete_Entry (curr_sel);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	switch (ctrl_id)
	{
		case IDC_ADD_MAP_BUTTON:
			Add_Map ();
			break;

		case IDC_REMOVE_MAP_BUTTON:
			Remove_Map ();
			break;
	}

	ChildDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_ListCtrl_DblClk
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::On_ListCtrl_DblClk
(
	ListCtrlClass *list_ctrl,
	int				ctrl_id,
	int				item_index
)
{
	switch (ctrl_id)
	{
		case IDC_AVAILABLE_MAP_LIST_CTRL:
			Add_Map ();
			break;

		case IDC_MAP_CYCLE_LIST_CTRL:
			Remove_Map ();
			break;
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_ComboBoxCtrl_Sel_Change
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::On_ComboBoxCtrl_Sel_Change
(
	ComboBoxCtrlClass *	combo_ctrl,
	int						ctrl_id,
	int						old_sel,
	int						new_sel
)
{
	Populate_Map_List_Ctrl ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Populate_Map_List_Ctrl
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Populate_Map_List_Ctrl (void)
{
	ComboBoxCtrlClass *combobox_ctrl = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_MOD_PACKAGE_COMBO);
	if (combobox_ctrl == NULL) {
		return ;
	}

	//
	//	Lookup the currently selected mod package
	//
	int curr_sel = combobox_ctrl->Get_Curr_Sel ();
	if (curr_sel != -1) {
		const ModPackageClass *package = (const ModPackageClass *)combobox_ctrl->Get_Item_Data (curr_sel);

		//
		//	Build a list of levels from the current mod package (or without a mod package)
		//
		Build_Map_List (package);

		//
		//	Fill the list controls with map names
		//
		Fill_Map_Ctrls ();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Fill_Map_Ctrls
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Fill_Map_Ctrls (void)
{
	//
	// Build map cycle list from ini file.
	// If map exists, add it to MapCycleList and remove it from MapList
	//
	MapCycleList.Delete_All();
	for (int i = 0; i < cGameData::MAX_MAPS; i++)
	{
		//
		//	Get the map name as a wide character string
		//
		StringClass ascii_map_name = The_Game ()->Get_Map_Cycle (i);
		WideStringClass map_name;
		map_name.Convert_From (ascii_map_name);

		if (map_name.Get_Length () > 0 && List_Contains(MapList, map_name)) {
			MapList.Delete (map_name);
			MapCycleList.Add (map_name);
		}
	}

	//
	//	Add the first map by default
	//
	if (MapCycleList.Count () == 0 && MapList.Count () > 0) {
		MapCycleList.Add (MapList[0]);
		MapList.Delete (0);
	}

	//
	//	Create the available maps list
	//
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_AVAILABLE_MAP_LIST_CTRL);
	if (list_ctrl != NULL) {

		//
		//	Start fresh
		//
		list_ctrl->Delete_All_Entries ();

		//
		//	Add the map list into the list ctrl
		//
		for (int index = 0; index < MapList.Count (); index ++) {
			list_ctrl->Insert_Entry (index, MapList[index]);
		}

		//
		//	Select the first entry by default
		//
		list_ctrl->Set_Curr_Sel (0);
	}

	//
	//	Create the map cycle list
	//
	list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_MAP_CYCLE_LIST_CTRL);
	if (list_ctrl != NULL) {

		//
		//	Start fresh
		//
		list_ctrl->Delete_All_Entries ();

		//
		//	Add the map cycle list into the list ctrl
		//
		for (int index = 0; index < MapCycleList.Count (); index ++) {
			list_ctrl->Insert_Entry (index, MapCycleList[index]);
		}

		//
		//	Select the first entry by default
		//
		list_ctrl->Set_Curr_Sel (0);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Build_Map_List
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Build_Map_List (const ModPackageClass *package)
{
	if (package == NULL) {
		Build_Map_List ();
	} else {

		//
		//	Generate a list of levels from the mod package
		//
		DynamicVectorClass<StringClass> map_list;
		package->Build_Level_List (map_list);

		//
		//	Copy the map list
		//
		MapList.Delete_All ();
		for (int index = 0; index < map_list.Count (); index ++) {
			WideStringClass wide_map_name;
			wide_map_name.Convert_From (map_list[index]);
			MapList.Add (wide_map_name);
		}
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Build_Map_List
//
////////////////////////////////////////////////////////////////
void
MPLanHostMapCycleOptionsTabClass::Build_Map_List (void)
{
	MapList.Delete_All ();

	WIN32_FIND_DATA find_info	= { 0 };
	BOOL keep_going				= TRUE;
	HANDLE file_find				= NULL;

	//
	//	Build a list of all the maps we know about
	//

	StringClass file_filter;
	if (The_Game()->Is_Cnc()) {
		file_filter.Format("data/c&c_*.mix");
	} else {
		file_filter.Format("data/mp_*.mix");
	}

	for (file_find = ::FindFirstFile (file_filter, &find_info);
		 (file_find != INVALID_HANDLE_VALUE) && keep_going;
		  keep_going = ::FindNextFile (file_find, &find_info))
	{
		//
		//	Convert the string to a wide character format
		//
		WideStringClass map_name;
		map_name.Convert_From (find_info.cFileName);

		//
		//	Add this name to our list
		//
		MapList.Add (map_name);
	}

	if (file_find != INVALID_HANDLE_VALUE) {
		::FindClose (file_find);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	MPLanHostVictoryOptionsTabClass
//
////////////////////////////////////////////////////////////////
MPLanHostVictoryOptionsTabClass::MPLanHostVictoryOptionsTabClass (void)	:
	ChildDialogClass (IDD_MP_LAN_HOST_OPTIONS_VICTORY_TAB)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostVictoryOptionsTabClass::On_Init_Dialog (void)
{
	//
	//	Configure the score and life/kill limit controls
	//
	Update_Enable_State ();

	//
	//	Set the check state of the CnC mode controls
	//
	cGameDataCnc *cnc_mode = The_Game ()->As_Cnc ();
	if (cnc_mode != NULL) {
		Check_Dlg_Button (IDC_DESTROY_ALL_BUILDINGS_CHECK,	cnc_mode->BaseDestructionEndsGame.Get());
		Check_Dlg_Button (IDC_BEACON_CHECK,						cnc_mode->BeaconPlacementEndsGame.Get());
		Update_Enable_State();
	} else {
		Check_Dlg_Button (IDC_DESTROY_ALL_BUILDINGS_CHECK,	false);
		Check_Dlg_Button (IDC_BEACON_CHECK,						false);
		Enable_Dlg_Item (IDC_DESTROY_ALL_BUILDINGS_CHECK,	false);
		Enable_Dlg_Item (IDC_BEACON_CHECK,						false);
	}

	ChildDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Apply
//
////////////////////////////////////////////////////////////////
bool
MPLanHostVictoryOptionsTabClass::On_Apply (void)
{
	//
	//	Pass our settings onto the game
	//

	//
	//	Save any CnC mode specific data
	//
	cGameDataCnc *cnc_mode = The_Game ()->As_Cnc ();
	if (cnc_mode != NULL) {
		//Check_Dlg_Button (IDC_DESTROY_ALL_BUILDINGS_CHECK,	true);
		//Check_Dlg_Button (IDC_BEACON_CHECK,						true);
		cnc_mode->BaseDestructionEndsGame.Set(Is_Dlg_Button_Checked(IDC_DESTROY_ALL_BUILDINGS_CHECK));
		cnc_mode->BeaconPlacementEndsGame.Set(Is_Dlg_Button_Checked(IDC_BEACON_CHECK));
	}

	return true;
}

////////////////////////////////////////////////////////////////
//
//	Update_Enable_State
//
////////////////////////////////////////////////////////////////
void
MPLanHostVictoryOptionsTabClass::Update_Enable_State (void)
{
	/*
	bool enable = Is_Dlg_Button_Checked (IDC_SCORE_LIMIT_CHECK);
	Enable_Dlg_Item (IDC_SCORE_LIMIT_EDIT,	enable);

	enable = Is_Dlg_Button_Checked (IDC_KILL_LIMIT_CHECK);
	Enable_Dlg_Item (IDC_KILL_LIMIT_EDIT,	enable);
	*/

	//
	// Beacon option dependent on base destruction option
	//
	bool enable = Is_Dlg_Button_Checked (IDC_DESTROY_ALL_BUILDINGS_CHECK);
	Enable_Dlg_Item (IDC_BEACON_CHECK,	enable);

	//
	//	Make sure to uncheck the beacon check if its disabled...
	//
	if (enable == false) {
		Check_Dlg_Button (IDC_BEACON_CHECK,	false);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Command
//
////////////////////////////////////////////////////////////////
void
MPLanHostVictoryOptionsTabClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	/*
	switch (ctrl_id)
	{
		case IDC_SCORE_LIMIT_CHECK:
		case IDC_KILL_LIMIT_CHECK:
			Update_Enable_State ();
			break;
	}
	*/

	switch (ctrl_id)
	{
		case IDC_DESTROY_ALL_BUILDINGS_CHECK:
			Update_Enable_State ();
			break;
	}

	ChildDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
MPLanHostCnCOptionsTabClass::On_Init_Dialog (void)
{
	cGameDataCnc *game_data = The_Game ()->As_Cnc ();

	//
	//	Configure the edit control
	//
	EditCtrlClass * edit = (EditCtrlClass *)Get_Dlg_Item(IDC_STARTING_CREDITS_EDIT);
	if (edit != NULL)
	{
		edit->Set_Text_Limit(5);
	}

	Set_Dlg_Item_Int (IDC_STARTING_CREDITS_EDIT,	game_data->Get_Starting_Credits ());

	//
	//	Configure the checkboxes
	//
	Check_Dlg_Button (IDC_CAN_REPAIR_BUILDINGS_CHECK, The_Game ()->CanRepairBuildings.Is_True ());
	Check_Dlg_Button (IDC_DRIVER_IS_ALWAYS_GUNNER_CHECK, The_Game ()->DriverIsAlwaysGunner.Is_True ());
	Check_Dlg_Button (IDC_SPAWN_WEAPONS_CHECK, The_Game ()->SpawnWeapons.Is_True ());
	Check_Dlg_Button (IDC_ALLIED_FIRE_CHECK, The_Game ()->IsFriendlyFirePermitted.Is_True ());
	Enable_Dlg_Item (IDC_ALLIED_FIRE_CHECK, The_Game ()->Is_Editable_Friendly_Fire ());

	//
	//	Configure the radar mode combobox
	//
	ComboBoxCtrlClass *radar_combobox = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_RADAR_MODE_COMBO);
	if (radar_combobox != NULL) {

		//
		//	Fill the radar_combobox
		//
		radar_combobox->Add_String (TRANSLATION(IDS_MP_RADAR_MODE_NOBODY));
		radar_combobox->Add_String (TRANSLATION(IDS_MP_RADAR_MODE_TEAMMATES));
		radar_combobox->Add_String (TRANSLATION(IDS_MP_RADAR_MODE_ALL));

		//
		//	Select the default entry in the radar_combobox
		//
		radar_combobox->Set_Curr_Sel (The_Game ()->Get_Radar_Mode ());
	}

	ChildDialogClass::On_Init_Dialog ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Apply
//
////////////////////////////////////////////////////////////////
bool
MPLanHostCnCOptionsTabClass::On_Apply (void)
{
	cGameDataCnc *game_data = The_Game ()->As_Cnc ();

	//
	//	Read the edit control
	//
	game_data->Set_Starting_Credits (Get_Dlg_Item_Int (IDC_STARTING_CREDITS_EDIT));

	//
	//	Read the checkboxes
	//
	The_Game ()->CanRepairBuildings.Set (Is_Dlg_Button_Checked (IDC_CAN_REPAIR_BUILDINGS_CHECK));
	The_Game ()->DriverIsAlwaysGunner.Set (Is_Dlg_Button_Checked (IDC_DRIVER_IS_ALWAYS_GUNNER_CHECK));
	The_Game ()->SpawnWeapons.Set (Is_Dlg_Button_Checked (IDC_SPAWN_WEAPONS_CHECK));
	The_Game ()->IsFriendlyFirePermitted.Set (Is_Dlg_Button_Checked (IDC_ALLIED_FIRE_CHECK));

	//
	//	Read the radar mode combobox
	//
	ComboBoxCtrlClass *radar_combobox = (ComboBoxCtrlClass *)Get_Dlg_Item (IDC_RADAR_MODE_COMBO);
	if (radar_combobox != NULL) {
		The_Game ()->Set_Radar_Mode ((RadarModeEnum)radar_combobox->Get_Curr_Sel ());
	}

	return true;
}
