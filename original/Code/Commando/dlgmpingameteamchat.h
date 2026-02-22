#pragma once

#include "global.h"

#include "popupdialog.h"
#include "resource.h"

////////////////////////////////////////////////////////////////
//
//	MPIngameTeamChatPopupClass
//
////////////////////////////////////////////////////////////////
class MPIngameTeamChatPopupClass : public PopupDialogClass
{
public:
	MPIngameTeamChatPopupClass (void)	:
		PopupDialogClass (IDD_MULTIPLAY_INGAME_TEAM_CHAT)	{}

	void	On_Init_Dialog (void);
	bool	On_Key_Down (uint32_t key_id, uint32_t key_data);

private:
};
