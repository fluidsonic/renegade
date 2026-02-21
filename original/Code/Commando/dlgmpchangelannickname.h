#include "Resource.h"
#include <PopupDialog.h>

class DlgMpChangeLanNickname : public PopupDialogClass
{
public:
	static bool DoDialog(void);

protected:
	DlgMpChangeLanNickname();
	~DlgMpChangeLanNickname();

	void On_Init_Dialog(void);
	void On_Command(int ctrlID, int mesage, DWORD param);
	void On_EditCtrl_Change(EditCtrlClass* edit, int id);
	void On_EditCtrl_Enter_Pressed(EditCtrlClass* edit, int id);

	static int DialogCount;
};

