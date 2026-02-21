#include "DlgMPConnectionRefused.h"
#include "cnetwork.h"
#include "resource.h"
#include <wwdebug/wwdebug.h>
#include "dlgmainmenu.h"
#include "gamespyadmin.h"
#include "specialbuilds.h"
#include "dialogtests.h"
#include "dialogmgr.h"
#include "gameinitmgr.h"

/******************************************************************************
*
* NAME
*     DlgMPConnectionRefused::DoDialog
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

bool DlgMPConnectionRefused::DoDialog(const WCHAR * text, bool show_splash_screen)
	{	
	DlgMPConnectionRefused* popup = new DlgMPConnectionRefused(text, show_splash_screen);

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
*     DlgMPConnectionRefused::DlgMPConnectionRefused
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

DlgMPConnectionRefused::DlgMPConnectionRefused(const WCHAR * text, bool show_splash_screen) :
		PopupDialogClass(IDD_MULTIPLAY_CONNECTION_REFUSED)
	{

	Text.Format(text);

	ShowSplashScreen = show_splash_screen;
	}


/******************************************************************************
*
* NAME
*     DlgMPConnectionRefused::~DlgMPConnectionRefused
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

DlgMPConnectionRefused::~DlgMPConnectionRefused()
	{
	}


/******************************************************************************
*
* NAME
*     DlgMPConnectionRefused::On_Init_Dialog
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

void DlgMPConnectionRefused::On_Init_Dialog(void)
	{
	Set_Dlg_Item_Text(IDC_REFUSAL_TEXT, Text);

	PopupDialogClass::On_Init_Dialog();
	}


/******************************************************************************
*
* NAME
*     DlgMPConnectionRefused::On_Command
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

void DlgMPConnectionRefused::On_Command(int ctrlID, int message, DWORD param)
	{
	if ((IDOK == ctrlID) && (1 != param))
		{
		if (cNetwork::I_Am_Client())
			{
			cNetwork::Cleanup_Client();
			}
		if (cGameSpyAdmin::Get_Is_Launched_From_Gamespy()) 
			{

#ifdef MULTIPLAYERDEMO
			GameInitMgrClass::End_Game ();
			if (ShowSplashScreen)
			{
				DialogMgrClass::Flush_Dialogs ();
				START_DIALOG (SplashOutroMenuDialogClass);
			}
			else 
			{
				extern void Stop_Main_Loop (int);
				Stop_Main_Loop(EXIT_SUCCESS);
			}
#else
			extern void Stop_Main_Loop (int);
			Stop_Main_Loop(EXIT_SUCCESS);
#endif // MULTIPLAYERDEMO
			}

		else if (DialogMgrClass::Get_Dialog_Count () == 1)
			{
			START_DIALOG (MainMenuDialogClass);
			}
		}

	PopupDialogClass::On_Command(ctrlID, message, param);
	}
