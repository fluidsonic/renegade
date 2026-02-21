#include "dlgmpchangelannickname.h"
#include <EditCtrl.h>
#include "netinterface.h"
#include "dlgmplangamelist.h"

int	DlgMpChangeLanNickname::DialogCount	= 0;

//-----------------------------------------------------------------------------
bool DlgMpChangeLanNickname::DoDialog(void)
{	
	DlgMpChangeLanNickname* dialog = NULL;

	if (DialogCount == 0) {
		dialog = new DlgMpChangeLanNickname;
	}

	if (dialog)
	{
		dialog->Start_Dialog();
		dialog->Release_Ref();
	}

	return (dialog != NULL);
}

//-----------------------------------------------------------------------------
DlgMpChangeLanNickname::DlgMpChangeLanNickname() :
	PopupDialogClass(IDD_MP_CHANGE_LAN_NICKNAME)
{

	DialogCount++;
}

//-----------------------------------------------------------------------------
DlgMpChangeLanNickname::~DlgMpChangeLanNickname()
{

	DialogCount--;
}

//-----------------------------------------------------------------------------
void DlgMpChangeLanNickname::On_Init_Dialog(void)
{
	Enable_Dlg_Item(IDOK, false);

	EditCtrlClass* edit = (EditCtrlClass*)Get_Dlg_Item(IDC_NICKNAME_EDIT);

	if (edit)
	{
		edit->Set_Text_Limit(9);
		edit->Set_Text(cNetInterface::Get_Nickname());
		edit->Set_Focus();
	}

	PopupDialogClass::On_Init_Dialog();
}

//-----------------------------------------------------------------------------
void DlgMpChangeLanNickname::On_Command(int ctrlID, int message, DWORD param)
{
	if (IDOK == ctrlID)
	{
		WideStringClass nickname = Get_Dlg_Item_Text (IDC_NICKNAME_EDIT);
		cNetInterface::Set_Nickname (nickname);	

		MPLanGameListMenuClass::Set_Update_Nickname();

		End_Dialog();
	}

	PopupDialogClass::On_Command(ctrlID, message, param);
}

//-----------------------------------------------------------------------------
void DlgMpChangeLanNickname::On_EditCtrl_Change(EditCtrlClass* edit, int id)
{
	if (IDC_NICKNAME_EDIT == id)
	{
		const WCHAR* text = edit->Get_Text();
		bool enableok = (text && (wcslen(text) > 0));
		Enable_Dlg_Item(IDOK, enableok);
	}
}

//-----------------------------------------------------------------------------
void DlgMpChangeLanNickname::On_EditCtrl_Enter_Pressed(EditCtrlClass* edit, int id)
{
	if ((IDC_NICKNAME_EDIT == id) && Is_Dlg_Item_Enabled(IDOK))
	{
		On_Command(IDOK, 0, 0);
	}
}
