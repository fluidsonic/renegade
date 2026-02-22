#include "dlgpreviewoptions.h"
#include "listctrl.h"

////////////////////////////////////////////////////////////////
//
//	PreviewOptionsMenuClass
//
////////////////////////////////////////////////////////////////
PreviewOptionsMenuClass::PreviewOptionsMenuClass (void)	:
	MenuDialogClass (IDD_OPTIONS_PREVIEW)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Init_Dialog
//
////////////////////////////////////////////////////////////////
void
PreviewOptionsMenuClass::On_Init_Dialog (void)
{
	//
	//	Get a pointer to the list control
	//
	ListCtrlClass *list_ctrl = (ListCtrlClass *)Get_Dlg_Item (IDC_LIST_CTRL);
	if (list_ctrl != NULL) {

		//
		//	Configure the list control
		//
		list_ctrl->Add_Column (u"Preview Title", 1.0F, Vector3 (1, 1, 1));
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
PreviewOptionsMenuClass::On_Command (int ctrl_id, int message_id, DWORD param)
{
	MenuDialogClass::On_Command (ctrl_id, message_id, param);
	return ;
}

