#include "Resource.h"
#include <PopupDialog.h>
#include <Signaler.h>

class DlgPasswordPrompt :
		public PopupDialogClass,
		public Signaler<DlgPasswordPrompt>
	{
	public:
		static bool DoDialog(Signaler<DlgPasswordPrompt>* target);

		const WCHAR* GetPassword(void) const;

	protected:
		DlgPasswordPrompt();
		~DlgPasswordPrompt();

		// Prevent copy and assignment
		DlgPasswordPrompt(const DlgPasswordPrompt&);
		const DlgPasswordPrompt& operator=(const DlgPasswordPrompt&);

		void On_Init_Dialog(void);
		void On_Command(int ctrlID, int mesage, DWORD param);
		void On_EditCtrl_Change(EditCtrlClass* edit, int id);
		void On_EditCtrl_Enter_Pressed(EditCtrlClass* edit, int id);
	};


