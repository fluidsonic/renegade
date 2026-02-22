#pragma once

#include "global.h"

#include "PlayerManager.h"
#include <MenuDialog.h>
#include <Notify.h>
#include <Signaler.h>

class cPlayer;

typedef TypedEventPair<bool, int> MPChooseTeamSignal;

class DlgMPTeamSelect :
		public MenuDialogClass,
		protected Signaler<MPChooseTeamSignal>,
		protected Observer<PlayerMgrEvent>
	{
	public:
		static void DoDialog(Signaler<MPChooseTeamSignal>& target);

	protected:
		DlgMPTeamSelect(void);
		~DlgMPTeamSelect();

		bool FinalizeCreate(void);

		void On_Init_Dialog(void);
		void On_Frame_Update(void);
		void On_Command(int ctrlID, int message, DWORD param);
		void On_Last_Menu_Ending(void);

		void InitSideChoice(int sidePref);
		void SelectSideChoice(int side);
		int GetSideChoice(void);

		void ShowTimeRemaining(float remainingSecond);
		bool FindPlayerInListCtrl(const WCHAR* name, ListCtrlClass*& outList, int& outIndex);

		void HandleNotification(PlayerMgrEvent&);

		void PopulateWithLANPlayers(void);
		void AddLANPlayerInfo(cPlayer* lanPlayer);
		void RemoveLANPlayerInfo(cPlayer* lanPlayer);

	protected:
		bool mCanChoose;
		float mTimeRemaining;
	};
