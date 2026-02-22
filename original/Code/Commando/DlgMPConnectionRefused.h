#pragma once

#include "global.h"

#include <PopupDialog.h>

class DlgMPConnectionRefused :
		public PopupDialogClass
	{
	public:
		static bool DoDialog(const WCHAR * text, bool show_splash_screen);
		void On_Init_Dialog(void);

	protected:
		DlgMPConnectionRefused(const WCHAR * text, bool show_splash_screen);
		virtual ~DlgMPConnectionRefused();

		void On_Command(int ctrl, int message, DWORD param);

	private:
		// Prevent copy and assignment
		DlgMPConnectionRefused(const DlgMPConnectionRefused&);
		const DlgMPConnectionRefused& operator=(const DlgMPConnectionRefused&);

		WideStringClass	Text;
		bool					ShowSplashScreen;
	};
