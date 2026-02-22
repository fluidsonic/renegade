#include "global.h"
#include "sctextobj.h"


#include "networkobjectfactory.h"
#include "textdisplay.h"
#include "wwaudio.h"
#include "colors.h"
#include "playermanager.h"
#include "matrix3d.h"
#include "playertype.h"
#include "cnetwork.h"
#include "translatedb.h"
#include "string_ids.h"
#include "apppackettypes.h"
#include "messagewindow.h"
#include "dlgmessagebox.h"
#include "consolemode.h"
#include "mpsettingsmgr.h"

DECLARE_NETWORKOBJECT_FACTORY(cScTextObj, NETCLASSID_SCTEXTOBJ);

//-----------------------------------------------------------------------------
cScTextObj::cScTextObj(void)
{
	SenderId					= HOST_TEXT_SENDER;
	RecipientId				= HOST_TEXT_SENDER;
	Type						= TEXT_MESSAGE_PUBLIC;
	IsHostAdminMessage	= false;

	Set_App_Packet_Type(APPPACKETTYPE_SCTEXTOBJ);
}

//-----------------------------------------------------------------------------
void
cScTextObj::Init
(
	const WideStringClass &		text,
	TextMessageEnum				type,
	bool								is_host_admin_message,
	int								sender_id,
	int								recipient_id
)
{

	Text						= text;
	Type						= type;
	SenderId					= sender_id;
	RecipientId				= recipient_id;
	IsHostAdminMessage	= is_host_admin_message;

	switch (Type)
	{
		case TEXT_MESSAGE_PUBLIC:
		{
			Set_Object_Dirty_Bit(BIT_CREATION, true);
			break;
		}

		case TEXT_MESSAGE_TEAM:
		{
			//assert(The_Game()->Is_Team_Game());

			cPlayer * p_sender = cPlayerManager::Find_Player(SenderId);

			int team = p_sender->Get_Player_Type();
			Set_Dirty_Bit_For_Team(BIT_CREATION, team);

			break;
		}

		case TEXT_MESSAGE_PRIVATE:
		{
			if (RecipientId >= 0) {
				Set_Object_Dirty_Bit(RecipientId, BIT_CREATION, true);
			}
			break;
		}

		default:
	}

	//
	// The sender always gets to see his own sent messages
	//
	if (SenderId != HOST_TEXT_SENDER)
	{
		Set_Object_Dirty_Bit(SenderId, BIT_CREATION, true);
	}

	//
	// Explicitly show the message on the server if he is among the recipients.
	// We also need to show it if he is dedicated and it is a public message, because
	// it may be a response to one of his own important messages.
	//
	if ((cNetwork::I_Am_Client() && Is_Client_Dirty(cNetwork::Get_My_Id())) ||
		 //(cNetwork::I_Am_Only_Server() && Type == TEXT_MESSAGE_PUBLIC) ||
		 (SenderId == HOST_TEXT_SENDER) ||
		 (RecipientId == HOST_TEXT_SENDER))
	{
		bool allow_act = true;

		//
		//	Check to see if the server is on the same team as the sender
		// before displaying the message
		//
		if (Type == TEXT_MESSAGE_TEAM) {
			allow_act = false;

			//
			//	Lookup the player that's sending the message
			//
			cPlayer *p_sender = cPlayerManager::Find_Player (SenderId);
			if (p_sender != NULL && p_sender->Is_Team_Player () && COMBAT_STAR != NULL) {
				
				//
				//	Is the player on the same team as the local player?
				//                         
				if (p_sender->Get_Player_Type() == COMBAT_STAR->Get_Player_Type()) {
					allow_act = true;
				}
			} else {
				if (cNetwork::I_Am_Only_Server() && ConsoleBox.Is_Exclusive() && (The_Game() && The_Game()->IsClanGame.Is_False())) {
					allow_act = true;
				}
			}
		}

		//
		//	Display the message
		//
		if (allow_act) {
			Act();
		}
	}

	//
	// This is a transient object. It is created, rendered, and destroyed immediately.
	//
	//Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScTextObj::Set_Dirty_Bit_For_Team(DIRTY_BIT bit, int team)
{

	for (
		SLNode<cPlayer> * player_node = cPlayerManager::Get_Player_Object_List()->Head();
		player_node != NULL;
		player_node = player_node->Next())
	{
		cPlayer * p_player = player_node->Data();

		if (p_player->Get_Is_Active().Is_True() &&
			 p_player->Get_Player_Type() == team)
		{
			Set_Object_Dirty_Bit(p_player->Get_Id(), bit, true);
		}
	}
}

//-----------------------------------------------------------------------------
void
cScTextObj::Act(void)
{
	bool isAnsi = Text.Is_ANSI();
	bool showAsian = MPSettingsMgrClass::Get_Option_Flag(MPSettingsMgrClass::OPTION_DISPLAY_ASIAN);
	bool showLatin = MPSettingsMgrClass::Get_Option_Flag(MPSettingsMgrClass::OPTION_DISPLAY_NONASIAN);

	if ((isAnsi && showLatin) || (!isAnsi && showAsian)) {
		WideStringClass sender_name;
		WideStringClass recipient_name;
		Vector3 sender_color;

		//
		// Determine the sender name
		//
		if (SenderId == HOST_TEXT_SENDER)
		{
			sender_name = TRANSLATION(IDS_MP_HOST);
			sender_color = COLOR_PUBLIC_TEXT;
		}
		else
		{
				cPlayer * p_sender = cPlayerManager::Find_Player(SenderId);
				if (p_sender != NULL)
			{
				sender_name = p_sender->Get_Name();
				sender_color = p_sender->Get_Color();
			}
		}

		//
		// Determine the recipient name
		//
		if (RecipientId == HOST_TEXT_SENDER)
		{
			recipient_name = TRANSLATION(IDS_MP_HOST);
		}
		else
		{
				cPlayer * p_recipient = cPlayerManager::Find_Player(RecipientId);
				if (p_recipient != NULL)
			{
				recipient_name = p_recipient->Get_Name();
			}
		}

		if (!sender_name.Is_Empty() && !recipient_name.Is_Empty())
		{
			StringClass sound_name;
			Vector3 text_color;

			switch (Type)
			{
				case TEXT_MESSAGE_PUBLIC:
					sound_name = "Public_Message";
					text_color = COLOR_PUBLIC_TEXT;
					break;

				case TEXT_MESSAGE_TEAM:
					sound_name = "Team_Message";
					text_color = sender_color;
					break;

				case TEXT_MESSAGE_PRIVATE:
					sound_name = "Private_Message";
					text_color = COLOR_PRIVATE_TEXT;
					break;

				default:
			}

			WWAudioClass::Get_Instance()->Create_Instant_Sound(sound_name, Matrix3D(1));

			WideStringClass formatted_text;
			if (Type == TEXT_MESSAGE_PRIVATE)
			{
				formatted_text.Format(u"%s (%s %s): ",
					(const WCHAR*)sender_name,
					TRANSLATION(IDS_MP_TO),
					(const WCHAR*)recipient_name);
			}
			else
			{
				formatted_text.Format(u"%s: ", (const WCHAR*)sender_name);
			}

			//
			//	Display the message...
			//
			/*
			Get_Text_Display()->Print(formatted_text, sender_color);
			formatted_text.Format(u"%s\n", Text);
			Get_Text_Display()->Print(formatted_text, text_color);
			*/

			/*
			WideStringClass message;
			message.Format(u"%s\n", Text);
			formatted_text += message;
			CombatManager::Get_Message_Window ()->Add_Message (formatted_text, text_color);
			*/

			bool do_popup = IsHostAdminMessage && cNetwork::I_Am_Client();
			if (do_popup && cNetwork::I_Am_Server() && Type == TEXT_MESSAGE_PRIVATE && 
				RecipientId != cNetwork::Get_My_Id())
			{
				do_popup = false;
			}

			if (do_popup) {
				DlgMsgBox::DoDialog(TRANSLATE(IDS_MENU_ADMIN_MESSAGE), Text);
			} else {
				WideStringClass message;
				message.Format(u"%s\n", (const WCHAR*)Text);
				formatted_text += message;
				if (CombatManager::Get_Message_Window() != NULL) {
					CombatManager::Get_Message_Window()->Add_Message(formatted_text, text_color);
				}

				if (isAnsi) {
					if (Type == TEXT_MESSAGE_PUBLIC) {
						ConsoleBox.Add_Message(&formatted_text, &text_color);
					} else {
						if (Type == TEXT_MESSAGE_TEAM) {
							if (cNetwork::I_Am_Only_Server() && ConsoleBox.Is_Exclusive() && (The_Game() && The_Game()->IsClanGame.Is_False())) {
								StringClass tempstr(true);
								StringClass teamstr("[Team] ", true);
								formatted_text.Convert_To(tempstr);
								teamstr += tempstr;
								ConsoleBox.Log_To_Disk(teamstr.Peek_Buffer());
							}	
						}
					}
				}
			}
		}
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScTextObj::Export_Creation(BitStreamClass & packet)
{
	cNetEvent::Export_Creation(packet);

	packet.Add((BYTE) Type);
	packet.Add(SenderId);
	packet.Add(RecipientId);
	packet.Add(IsHostAdminMessage);
	packet.Add_Wide_Terminated_String(Text);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScTextObj::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	BYTE type = packet.Get(type);
	Type = (TextMessageEnum) type;
	packet.Get(SenderId);
	packet.Get(RecipientId);
	packet.Get(IsHostAdminMessage);
	packet.Get_Wide_Terminated_String(Text.Get_Buffer(256), 256);

	Act();
}

		/*
		//
		// Let's play a "yo" if our name is in the text.
		//
		if (cNetwork::I_Am_Client()) {
			cPlayer * p_my_player = cNetwork::Get_My_Player_Object();
			if (p_my_player != NULL) {
				StringClass text;
				Text.Convert_To(text);
				StringClass name;
				p_my_player->Get_Name().Convert_To(name);
				if () {
				}
			}
		}
		*/
