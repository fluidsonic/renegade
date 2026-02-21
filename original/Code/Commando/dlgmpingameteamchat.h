#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLGMPINGAMETEAMCHAT_H__
#define __DLGMPINGAMETEAMCHAT_H__

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
	bool	On_Key_Down (uint32 key_id, uint32 key_data);

private:
};


#endif //__DLGMPINGAMETEAMCHAT_H__
