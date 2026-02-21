#include "DlgMessageBox.h"
#include "Resource.h"
#include <WWTranslateDB/TranslateDB.h>

//
// Class statics
//
int DlgMsgBox::CurrentCount	= 0;

/******************************************************************************
*
* NAME
*     DlgMsgBox::DoDialog
*
* DESCRIPTION
*     Create a Popup message box dialog.
*
* INPUTS
*     Title - Message box title
*     Text  - Message content
*
* RESULT
*      Object - Instance of message box
*
******************************************************************************/

bool DlgMsgBox::DoDialog(const WCHAR* title, const WCHAR* text,
		DlgMsgBox::Type type, Observer<DlgMsgBoxEvent>* observer, unsigned long user_data)
	{	
	DlgMsgBox* popup = new DlgMsgBox;

	if (popup)
		{
		popup->SetResourceType(type);
		popup->Start_Dialog();

		popup->Set_Title(title);
		popup->Set_Dlg_Item_Text(IDC_MESSAGE, text);
		popup->Set_User_Data(user_data);

		if (observer)
			{
			popup->AddObserver(*observer);
			}

		popup->Release_Ref();
		}

	return (popup != NULL);
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::DoDialog
*
* DESCRIPTION
*     Create a Popup message box dialog.
*
* INPUTS
*     TitleID - ID of message box title
*     TextID  - ID of message content
*
* RESULT
*      Object - Instance of message box
*
******************************************************************************/

bool DlgMsgBox::DoDialog(int titleID, int textID,
		DlgMsgBox::Type type, Observer<DlgMsgBoxEvent>* observer, unsigned long user_data)
	{
	const WCHAR* title = TranslateDBClass::Get_String(titleID);
	const WCHAR* text = TranslateDBClass::Get_String(textID);
	return DoDialog(title, text, type, observer, user_data);
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::DlgMsgBox
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

DlgMsgBox::DlgMsgBox() :
		mUserData(0),
		PopupDialogClass(IDD_MESSAGEBOX_OK)
	{
		CurrentCount++;
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::~DlgMsgBox
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

DlgMsgBox::~DlgMsgBox()
	{
		CurrentCount--;
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::SetResourceType
*
* DESCRIPTION
*     Set the type of dialog box to use.
*
* INPUTS
*
* RESULT
*
******************************************************************************/

void DlgMsgBox::SetResourceType(DlgMsgBox::Type type)
	{
	static UINT _types[] =
		{
		IDD_MESSAGEBOX_OK,
		IDD_MESSAGEBOX_YESNO
		};

	DialogResID = _types[type];
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::End_Dialog
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

void DlgMsgBox::End_Dialog(void)
	{
	Add_Ref();
	DlgMsgBoxEvent event(DlgMsgBoxEvent::Quitting, this, mUserData);
	NotifyObservers(event);
	Release_Ref();

	PopupDialogClass::End_Dialog();
	}

/******************************************************************************
*
* NAME
*     DlgMsgBox::On_Command
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

void DlgMsgBox::On_Command(int ctrl, int message, DWORD param)
	{
	switch (ctrl)
		{
		case IDOK:
			{
			Add_Ref();
			
			DlgMsgBoxEvent event(DlgMsgBoxEvent::Okay, this, mUserData);
			NotifyObservers(event);
			
			Release_Ref();
			End_Dialog();
			}
			break;

		case IDYES:
			{
			Add_Ref();
			
			DlgMsgBoxEvent event(DlgMsgBoxEvent::Yes, this, mUserData);
			NotifyObservers(event);
			
			Release_Ref();
			End_Dialog();
			}
			break;

		case IDNO:
			{
			Add_Ref();
			
			DlgMsgBoxEvent event(DlgMsgBoxEvent::No, this, mUserData);
			NotifyObservers(event);

			Release_Ref();
			End_Dialog();
			}
			break;

		default:
			PopupDialogClass::On_Command(ctrl, message, param);
			break;
		}
	}
