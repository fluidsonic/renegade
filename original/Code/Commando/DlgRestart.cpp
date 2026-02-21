#include "DlgRestart.h"
#include "Resource.h"
#include "MainLoop.h"
#include "String_IDs.h"
#include <WWTranslateDB/TranslateDB.h>
#include <WWDebug/WWDebug.h>

/******************************************************************************
*
* NAME
*     DlgRestart::DoDialog
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

bool DlgRestart::DoDialog(void)
	{	
	DlgRestart* popup = new DlgRestart;

	if (popup)
		{
		popup->Start_Dialog();
		popup->Set_Title(TRANSLATE(IDS_RESTART_TITLE));
		popup->Release_Ref();
		}

	return (popup != NULL);
	}


/******************************************************************************
*
* NAME
*     DlgRestart::DlgRestart
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

DlgRestart::DlgRestart() :
		PopupDialogClass(IDD_MESSAGEBOX_OK)
	{
	}


/******************************************************************************
*
* NAME
*     DlgRestart::~DlgRestart
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

DlgRestart::~DlgRestart()
	{
	}


/******************************************************************************
*
* NAME
*     DlgRestart::On_Init_Dialog
*
* DESCRIPTION
*     One time initialzation.
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgRestart::On_Init_Dialog(void)
	{
	Set_Dlg_Item_Text(IDC_MESSAGE, TRANSLATE(IDS_RESTART_PROMPT));
	PopupDialogClass::On_Init_Dialog();
	}


/******************************************************************************
*
* NAME
*     DlgRestart::On_Command
*
* DESCRIPTION
*     Process command message
*
* INPUTS
*     Ctrl    - ID of control
*     Message -
*     Param   -
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgRestart::On_Command(int ctrl, int message, DWORD param)
	{
	if (IDOK == ctrl)
		{
		Stop_Main_Loop(RESTART_EXITCODE);
		End_Dialog();
		}
	}
