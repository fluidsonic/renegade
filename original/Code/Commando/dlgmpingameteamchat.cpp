#include "dlgmpingameteamchat.h"

#include "cnetwork.h"
#include "dialogcontrol.h"
#include "dialogmgr.h"
#include "gamedata.h"
#include "cstextobj.h"
#include "sctextobj.h"


////////////////////////////////////////////////////////////////
//
//	MPIngameTeamChatPopupClass
//
////////////////////////////////////////////////////////////////
void
MPIngameTeamChatPopupClass::On_Init_Dialog (void)
{
	WWASSERT(The_Game() != NULL);

	Set_Background_Darkened(false);

	PopupDialogClass::On_Init_Dialog ();
}


////////////////////////////////////////////////////////////////
//
//	On_Key_Down
//
////////////////////////////////////////////////////////////////
bool
MPIngameTeamChatPopupClass::On_Key_Down (uint32 key_id, uint32 key_data)
{
	bool retval = false;

	retval = PopupDialogClass::On_Key_Down (key_id, key_data);

	if (key_id == VK_RETURN)
	{
		WideStringClass msg = Get_Dlg_Item_Text(IDC_MULTIPLAY_INGAME_CHAT_EDIT);
		if (msg.Get_Length() > 0) 
		{
			cCsTextObj * p_message = new cCsTextObj;
			p_message->Init(msg, TEXT_MESSAGE_TEAM, cNetwork::Get_My_Id(), -1);
		}
		End_Dialog ();
	}
	
	return retval;
}

