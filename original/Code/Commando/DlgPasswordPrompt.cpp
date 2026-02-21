#include "DlgPasswordPrompt.h"
#include <EditCtrl.h>

/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::DoDialog
*
* DESCRIPTION
*     Create a dialog to prompt the user to enter a password.
*
* INPUTS
*     Target - Target to receive signal that a password was entered.
*
* RESULT
*     True if dialog created successfully.
*
******************************************************************************/

bool DlgPasswordPrompt::DoDialog(Signaler<DlgPasswordPrompt>* target)
	{	
	DlgPasswordPrompt* dialog = new DlgPasswordPrompt;

	if (dialog)
		{
		dialog->Start_Dialog();

		if (target)
			{
			dialog->SignalMe(*target);
			}

		dialog->Release_Ref();
		}

	return (dialog != NULL);
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::DlgPasswordPrompt
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

DlgPasswordPrompt::DlgPasswordPrompt() :
		PopupDialogClass(IDD_MP_JOIN_PASSWORD)
	{
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::~DlgPasswordPrompt
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

DlgPasswordPrompt::~DlgPasswordPrompt()
	{
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::GetPassword
*
* DESCRIPTION
*     Retrieve the password entered by the user.
*
* INPUTS
*     NONE
*
* RESULT
*     Password - Pointer to password string.
*
******************************************************************************/

const WCHAR* DlgPasswordPrompt::GetPassword(void) const
	{
	return Get_Dlg_Item_Text(IDC_PASSWORD_EDIT);
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::On_Init_Dialog
*
* DESCRIPTION
*     One time dialog initialization.
*
* INPUTS
*     NONE
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgPasswordPrompt::On_Init_Dialog(void)
	{
	// Disable the join button until the user enters text.
	Enable_Dlg_Item(IDC_JOIN_GAME_BUTTON, false);

	EditCtrlClass* edit = (EditCtrlClass*)Get_Dlg_Item(IDC_PASSWORD_EDIT);

	if (edit)
		{
		edit->Set_Focus();
		}

	PopupDialogClass::On_Init_Dialog();
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::On_Command
*
* DESCRIPTION
*     Handle command messages from dialog controls.
*
* INPUTS
*     CtrlID  - ID of control sending command.
*     Message - Message identifier.
*     Param   - Message parameter
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgPasswordPrompt::On_Command(int ctrlID, int message, DWORD param)
	{
	if (IDC_JOIN_GAME_BUTTON == ctrlID)
		{
		Signaler<DlgPasswordPrompt>::SendSignal(*this);
		End_Dialog();
		}

	PopupDialogClass::On_Command(ctrlID, message, param);
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::On_EditCtrl_Change
*
* DESCRIPTION
*     Handle notification that the contents of the edit control has changed.
*
* INPUTS
*     Edit - Pointer to edit control whose contents has changed.
*     ID   - Identifier of control.
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgPasswordPrompt::On_EditCtrl_Change(EditCtrlClass* edit, int id)
	{
	if (IDC_PASSWORD_EDIT == id)
		{
		const WCHAR* text = edit->Get_Text();
		bool enableJoin = (text && (wcslen(text) > 0));
		Enable_Dlg_Item(IDC_JOIN_GAME_BUTTON, enableJoin);
		}
	}


/******************************************************************************
*
* NAME
*     DlgPasswordPrompt::On_EditCtrl_Enter_Pressed
*
* DESCRIPTION
*     Handle notification that the enter key was pressed in an edit control.
*
* INPUTS
*     Edit - Pointer to edit control whose contents has changed.
*     ID   - Identifier of control.
*
* RESULT
*     NONE
*
******************************************************************************/

void DlgPasswordPrompt::On_EditCtrl_Enter_Pressed(EditCtrlClass* edit, int id)
	{
	if ((IDC_PASSWORD_EDIT == id) && Is_Dlg_Item_Enabled(IDC_JOIN_GAME_BUTTON))
		{
		On_Command(IDC_JOIN_GAME_BUTTON, 0, 0);
		}
	}
