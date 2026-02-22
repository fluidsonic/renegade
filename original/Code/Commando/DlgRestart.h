#pragma once

#include "global.h"

#include <PopupDialog.h>

class DlgRestart :
		public PopupDialogClass
	{
	public:
		static bool DoDialog(void);

	protected:
		DlgRestart();
		virtual ~DlgRestart();

		void On_Init_Dialog(void);
		void On_Command(int ctrl, int message, DWORD param);

	private:
		// Prevent copy and assignment
		DlgRestart(const DlgRestart&);
		const DlgRestart& operator=(const DlgRestart&);
	};
