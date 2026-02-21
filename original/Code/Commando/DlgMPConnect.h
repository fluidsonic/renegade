#ifndef __DLGMPCONNECT_H__
#define __DLGMPCONNECT_H__

#include <PopupDialog.h>

class cGameData;

class DlgMPConnect :
		public PopupDialogClass
	{
	public:
		// Display connecting dialog.
		//   TeamChoice - Team preference of connecting player
		static bool DoDialog(int teamChoice);

		void Connected(cGameData* theGame);
		void Failed_To_Connect(void);


	protected:
		DlgMPConnect(int teamChoice);
		virtual ~DlgMPConnect();

		void On_Command(int ctrl, int message, DWORD param);
		void On_Periodic(void);

	private:
		// Prevent copy and assignment
		DlgMPConnect(const DlgMPConnect&);
		const DlgMPConnect& operator=(const DlgMPConnect&);

		int mTeamChoice;

		cGameData* mTheGame;
		bool mFailed;
	};

#endif // __DLGMPCONNECT_H__