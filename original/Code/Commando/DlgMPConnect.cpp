#include "dlgmpconnect.h"
#include "gamedata.h"
#include "gameinitmgr.h"
#include "campaign.h"
#include "cnetwork.h"
#include "resource.h"
#include <wwdebug/wwdebug.h>
#include "dlgmainmenu.h"
#include "specialbuilds.h"
#include "dialogtests.h"
#include "dialogmgr.h"
#include "gamemode.h"
#include "langmode.h"

/******************************************************************************
*
* NAME
*     DlgMPConnect::DoDialog
*
* DESCRIPTION
*
* INPUTS
*     NONE
*
* RESULT
*     True if dialog created successfully.
*
******************************************************************************/

bool DlgMPConnect::DoDialog(int teamChoice)
	{
	DlgMPConnect* popup = new DlgMPConnect(teamChoice);

	if (popup)
		{
		popup->Start_Dialog();
		popup->Release_Ref();
		}

	return (popup != NULL);
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::DlgMPConnect
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

DlgMPConnect::DlgMPConnect(int teamChoice) :
		PopupDialogClass(IDD_MULTIPLAY_CONNECTING),
		mTeamChoice(teamChoice),
		mTheGame(NULL),
		mFailed(false)
	{
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::~DlgMPConnect
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

DlgMPConnect::~DlgMPConnect()
	{
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::Connected
*
* DESCRIPTION
*     Handle connection.
*
* INPUTS
*     GameData - Pointer to game data instance
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPConnect::Connected(cGameData* theGame)
	{
	mTheGame = theGame;
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::Connected
*
* DESCRIPTION
*     Handle connection.
*
* INPUTS
*     GameData - Pointer to game data instance
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPConnect::Failed_To_Connect(void)
	{
	mFailed = true;
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::On_Command
*
* DESCRIPTION
*     Process command messages from controls
*
* INPUTS
*     Ctrl    - ID of control
*     Message -
*     Param   -	0 = User invoked abort. 1 = Connection refused by server.
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgMPConnect::On_Command(int ctrlID, int message, DWORD param)
	{
	if ((IDCANCEL == ctrlID) && (1 != param))
		{
		if (cNetwork::I_Am_Client())
			{
			cNetwork::Cleanup_Client();
			}
		if (DialogMgrClass::Get_Dialog_Count () == 1)
			{
			START_DIALOG (MainMenuDialogClass);
			}
		}

	PopupDialogClass::On_Command(ctrlID, message, param);
	}


/******************************************************************************
*
* NAME
*     DlgMPConnect::On_Periodic
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

void DlgMPConnect::On_Periodic(void)
	{
	PopupDialogClass::On_Periodic();

	if (mTheGame != NULL)
		{
		// Add a reference to keep us alive while we process the game start
		Add_Ref();

		// Remove the dialog from menuing system
		End_Dialog();

		// Check to ensure the settings are playable
		WideStringClass outMsg;

		if (mTheGame->Is_Valid_Settings(outMsg))
			{

			CampaignManager::Select_Backdrop_Number_By_MP_Type(mTheGame->Get_Game_Type());

			// Start the game!
			GameInitMgrClass::Set_Is_Client_Required(true);
			GameInitMgrClass::Set_Is_Server_Required(false);
			GameInitMgrClass::Start_Game(mTheGame->Get_Map_Name(), mTeamChoice);
			}
		else
			{
			}

		// Release the keep alive reference (this will delete this object)
		Release_Ref();
		}
	else
		{
		if (mFailed)
			{

			Add_Ref();

			// Remove the dialog from menuing system
			End_Dialog();

			if (GameModeManager::Find("LAN")->Is_Active())
				{
      		PLC->Refusal_Actions();
   			}

			Release_Ref();
			}
		}
	}