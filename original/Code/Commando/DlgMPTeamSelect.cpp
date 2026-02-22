#include "dlgmpteamselect.h"
#include "gamedata.h"
#include "dlgmessagebox.h"
#include "renegadedialogmgr.h"
#include <combat/playertype.h>
#include <wwui/listctrl.h>
#include <wwui/comboboxctrl.h>
#include <wwui/imagectrl.h>
#include "resource.h"
#include "string_ids.h"
#include <wwtranslatedb/translatedb.h>
#include "modpackagemgr.h"
#include "gameinitmgr.h"
#include "gamemode.h"

// Player list columns
enum
	{
	COL_RANK,
	COL_NAME,
	COL_KD,
	COL_SCORE
	};

static int CALLBACK ListSortCallback(ListCtrlClass* list, int index1, int index2, uint32_t param)
	{
	int rank1 = (int)list->Get_Entry_Data(index1, COL_RANK);
	int rank2 = (int)list->Get_Entry_Data(index2, COL_RANK);
	return (rank1 - rank2);
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::DoDialog
*
* DESCRIPTION
*     Display team selection / team profile dialog
*
* INPUTS
*     Target - Target to receive user choice signal.
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::DoDialog(Signaler<MPChooseTeamSignal>& target)
	{
	DlgMPTeamSelect* dialog = new DlgMPTeamSelect;

	if (dialog)
		{
		if (dialog->FinalizeCreate())
			{
			dialog->Start_Dialog();
			dialog->SignalMe(target);
			}

		dialog->Release_Ref();
		}
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::DlgMpTeamSelect
*
* DESCRIPTION
*     Constructor
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

DlgMPTeamSelect::DlgMPTeamSelect(void) :
		MenuDialogClass(IDD_MP_TEAM_SELECT),
		mCanChoose(true),
		mTimeRemaining(0.0f)
	{
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::~DlgMPTeamSelect
*
* DESCRIPTION
*     Destructor
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

DlgMPTeamSelect::~DlgMPTeamSelect()
	{
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::FinalizeCreate
*
* DESCRIPTION
*     Post creation initialization.
*
* INPUTS
*     NONE
*
* RESULT
*     True if successful
*
******************************************************************************/

bool DlgMPTeamSelect::FinalizeCreate(void)
	{
	// WOL removed — in LAN mode team change is always allowed, clan games don't exist
	mCanChoose = true;

	return true;
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::On_Init_Dialog
*
* DESCRIPTION
*     One time dialog initialization
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::On_Init_Dialog(void)
	{

	MenuDialogClass::On_Init_Dialog();

	// Setup the team icons on the dialog
	((ImageCtrlClass*)Get_Dlg_Item(IDC_GDI_TEAM_ICON))->Set_Texture("HUD_C&C_GDILOGO.TGA");
	((ImageCtrlClass*)Get_Dlg_Item(IDC_NOD_TEAM_ICON))->Set_Texture("HUD_C&C_NODLOGO.TGA");

	// Setup the list controls
	ListCtrlClass* list = (ListCtrlClass*)Get_Dlg_Item(IDC_GDI_LIST_CTRL);

	if (list)
		{
		list->Add_Column(TRANSLATE (IDS_MENU_RANK),     0.15F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_NAME),     0.50F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_KD_RATIO), 0.15F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_SCORE),    0.20F, Vector3(1, 1, 1));
		}

	list = (ListCtrlClass*)Get_Dlg_Item(IDC_NOD_LIST_CTRL);

	if (list)
		{
		list->Add_Column(TRANSLATE (IDS_MENU_RANK),     0.15F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_NAME),     0.50F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_KD_RATIO), 0.15F, Vector3(1, 1, 1));
		list->Add_Column(TRANSLATE (IDS_MENU_SCORE),    0.20F, Vector3(1, 1, 1));
		}

	int sidePref = cNetInterface::Get_Side_Preference();

	// Hide and disable the back button for LAN games.
	DialogControlClass* ctrl = Get_Dlg_Item(IDCANCEL);
	ctrl->Show(false);
	ctrl->Enable(false);

	cPlayerManager::Add_Event_Observer(*this);

	PopulateWithLANPlayers();

	InitSideChoice(sidePref);

	//	Activate the menu game mode (if necessary)
	GameModeClass* menuMode = GameModeManager::Find("Menu");

	if (menuMode && !menuMode->Is_Active())
		{
		menuMode->Activate();
		}
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::On_Frame_Update
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

void DlgMPTeamSelect::On_Frame_Update(void)
	{
	if (mTimeRemaining >= WWMATH_EPSILON)
		{
		mTimeRemaining -= TimeManager::Get_Frame_Real_Seconds();
		ShowTimeRemaining(mTimeRemaining);
		}

	MenuDialogClass::On_Frame_Update();
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::On_Command
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::On_Command(int ctrlID, int message, DWORD param)
	{
	switch (ctrlID)
		{
		case IDCANCEL:
		case IDC_STARTGAME:
			{
			int side = GetSideChoice();
			MPChooseTeamSignal chooseSignal((IDC_STARTGAME == ctrlID), side);
			Signaler<MPChooseTeamSignal>::SendSignal(chooseSignal);
			End_Dialog();
			}
			break;

		case IDC_TEAM_AUTO_CHECK:
			SelectSideChoice(PLAYERTYPE_RENEGADE);
			break;

		case IDC_TEAM_GDI_CHECK:
			SelectSideChoice(PLAYERTYPE_GDI);
			break;

		case IDC_TEAM_NOD_CHECK:
			SelectSideChoice(PLAYERTYPE_NOD);
			break;
		}

	MenuDialogClass::On_Command(ctrlID, message, param);
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::On_Last_Menu_Ending
*
* DESCRIPTION
*     Callback from menu class signifying the close of the last menu
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::On_Last_Menu_Ending(void)
	{
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::InitSideChoice
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

void DlgMPTeamSelect::InitSideChoice(int sidePref)
	{
	Enable_Dlg_Item(IDC_TEAM_AUTO_CHECK, mCanChoose);
	Enable_Dlg_Item(IDC_TEAM_GDI_CHECK, mCanChoose);
	Enable_Dlg_Item(IDC_TEAM_NOD_CHECK, mCanChoose);

	// Default side selection combo to the users preference.
	int side = ((mCanChoose == true) ? sidePref : -1);
	SelectSideChoice(side);
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::SelectSideChoice
*
* DESCRIPTION
*
* INPUTS
*     Side -
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::SelectSideChoice(int side)
	{
	Check_Dlg_Button(IDC_TEAM_AUTO_CHECK, (PLAYERTYPE_RENEGADE == side));
	Check_Dlg_Button(IDC_TEAM_GDI_CHECK, (PLAYERTYPE_GDI == side));
	Check_Dlg_Button(IDC_TEAM_NOD_CHECK, (PLAYERTYPE_NOD == side));
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::GetSideChoice
*
* DESCRIPTION
*
* INPUTS
*     Side -
*
* RESULT
*     NONE
*
******************************************************************************/

int DlgMPTeamSelect::GetSideChoice(void)
	{
	if (Is_Dlg_Button_Checked(IDC_TEAM_GDI_CHECK))
		{
		return PLAYERTYPE_GDI;
		}
	else if (Is_Dlg_Button_Checked(IDC_TEAM_NOD_CHECK))
		{
		return PLAYERTYPE_NOD;
		}

	return PLAYERTYPE_RENEGADE;
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::ShowTimeRemaining
*
* DESCRIPTION
*     Set the time remaining text
*
* INPUTS
*     Seconds - Seconds remaining in the game
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPTeamSelect::ShowTimeRemaining(float remainingSeconds)
	{
	int hours = 0;
	int mins = 0;
	int seconds = 0;

	cMiscUtil::Seconds_To_Hms(remainingSeconds, hours, mins, seconds);

	WideStringClass timeString(0, true);
	timeString.Format(u"%02d:%02d:%02d", hours, mins, seconds);

	WideStringClass text(0, true);
	text.Format(u"%s: %s", TRANSLATION(IDS_MP_TIME_REMAINING), (const WCHAR*)timeString);
	Set_Dlg_Item_Text(IDC_TIME_REMAINING_TEXT, (const WCHAR*)text);
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::FindPlayerInListCtrl
*
* DESCRIPTION
*     Find a player entry in the player (GDI or Nod) list control.
*
* INPUTS
*     Name  - Name of player to look for.
*     List  - On return; List control containing player entry.
*     Index - On return; Player entry list index
*
* RESULT
*     True if player found in one of the lists.
*
******************************************************************************/

bool DlgMPTeamSelect::FindPlayerInListCtrl(const WCHAR* name, ListCtrlClass*& outList, int& outIndex)
	{
	// Check in GDI player list
	ListCtrlClass* list = (ListCtrlClass*)Get_Dlg_Item(IDC_GDI_LIST_CTRL);

	if (list)
		{
		int index = list->Find_Entry(COL_NAME, name);

		if (index != -1)
			{
			outList = list;
			outIndex = index;
			return true;
			}
		}

	// Check in Nod player list
	list = (ListCtrlClass*)Get_Dlg_Item(IDC_NOD_LIST_CTRL);

	if (list)
		{
		int index = list->Find_Entry(COL_NAME, name);

		if (index != -1)
			{
			outList = list;
			outIndex = index;
			return true;
			}
		}

	return false;
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::HandleNotification(PlayerMgrEvent)
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void DlgMPTeamSelect::HandleNotification(PlayerMgrEvent& event)
	{
	PLAYERMGR_ACTION action = event.GetAction();

	if ((action == PLAYER_ACTIVATED))
		{
		AddLANPlayerInfo(event.Subject());
		}
	else if ((action == PLAYER_DEACTIVATED) || (action == PLAYER_REMOVED))
		{
		RemoveLANPlayerInfo(event.Subject());
		}
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::PopulateWithLANPlayers
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

void DlgMPTeamSelect::PopulateWithLANPlayers(void)
	{
	SList<cPlayer>* playerList = cPlayerManager::Get_Player_Object_List();
	SLNode<cPlayer>* playerNode = playerList->Head();

	while (playerNode)
		{
		cPlayer* player = playerNode->Data();

		if (player->Get_Is_Active().Is_True())
			{
			AddLANPlayerInfo(player);
			}

		playerNode = playerNode->Next();
		}
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::AddLANPlayerInfo
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void DlgMPTeamSelect::AddLANPlayerInfo(cPlayer* player)
	{
	RemoveLANPlayerInfo(player);

	int playerType = player->Get_Player_Type();
	int listID = ((playerType == PLAYERTYPE_GDI) ? IDC_GDI_LIST_CTRL : IDC_NOD_LIST_CTRL);

	ListCtrlClass* list = (ListCtrlClass*)Get_Dlg_Item(listID);

	int itemIndex = list->Insert_Entry(list->Get_Entry_Count(), u"");

	if (itemIndex >= 0)
		{
		list->Set_Entry_Text(itemIndex, COL_NAME, player->Get_Name());
		list->Set_Entry_Int(itemIndex, COL_RANK, player->Get_Rung());
		list->Set_Entry_Int(itemIndex, COL_SCORE, player->Get_Score());

		WideStringClass text(0, true);
		text.Format(u"%d/%d", player->Get_Kills(), player->Get_Deaths());
		list->Set_Entry_Text(itemIndex, COL_KD, text);

		// If this is the player client the mark there name with a star
		const WideStringClass& nickname = cNetInterface::Get_Nickname();

		if (nickname.Compare_No_Case(player->Get_Name()) == 0)
			{
			list->Add_Icon(itemIndex, COL_NAME, "IF_LRGSTAR.TGA");
			list->Set_Entry_Color(itemIndex, COL_RANK, Vector3(1.0F, 1.0F, 1.0F));
			list->Set_Entry_Color(itemIndex, COL_NAME, Vector3(1.0F, 1.0F, 1.0F));
			list->Set_Entry_Color(itemIndex, COL_KD, Vector3(1.0F, 1.0F, 1.0F));
			list->Set_Entry_Color(itemIndex, COL_SCORE, Vector3(1.0F, 1.0F, 1.0F));

			SelectSideChoice(playerType);
			}
		}

	list->Sort(ListSortCallback, 0);
	}

/******************************************************************************
*
* NAME
*     DlgMPTeamSelect::RemoveLANPlayerInfo
*
* DESCRIPTION
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void DlgMPTeamSelect::RemoveLANPlayerInfo(cPlayer* player)
	{

	ListCtrlClass* list = NULL;
	int itemIndex = -1;
	bool found = FindPlayerInListCtrl(player->Get_Name(), list, itemIndex);

	if (found)
		{
		list->Delete_Entry(itemIndex);
		}
	}
