#include "global.h"
#include "playerkill.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "textdisplay.h"
#include "wwaudio.h"
#include "playermanager.h"
#include "matrix3d.h"
#include "cnetwork.h"
#include "gametype.h"
#include "translatedb.h"
#include "string_ids.h"
#include "apppackettypes.h"
#include "messagewindow.h"

DECLARE_NETWORKOBJECT_FACTORY(cPlayerKill, NETCLASSID_PLAYERKILL);

//-----------------------------------------------------------------------------
cPlayerKill::cPlayerKill(void)
{
	KillerId = PLAYER_ID_UNKNOWN;
	VictimId = PLAYER_ID_UNKNOWN;

	Set_App_Packet_Type(APPPACKETTYPE_PLAYERKILLEVENT);
}

//-----------------------------------------------------------------------------
void
cPlayerKill::Init(int killer_id, int victim_id)
{

	KillerId = killer_id;
	VictimId = victim_id;

	//
	// This skips the local client
	//
	Set_Object_Dirty_Bit(BIT_CREATION, true);

	if (cNetwork::I_Am_Client())
	{
		Act();
	}
}

//-----------------------------------------------------------------------------
void
cPlayerKill::Act(void)
{
	if (IS_MISSION || 
		GameModeManager::Find("Menu")->Is_Active() ||
		!GameModeManager::Find("Combat")->Is_Active()) {
		return;
	}

   WideStringClass	killer_name = TRANSLATION(IDS_MP_SOMEBODY);
   WideStringClass	victim_name = TRANSLATION(IDS_MP_SOMEBODY);

   cPlayer * p_killer = NULL;
   cPlayer * p_victim = NULL;

	if (KillerId != PLAYER_ID_UNKNOWN) {
      p_killer = cPlayerManager::Find_Player(KillerId);
		if (p_killer != NULL) {
			killer_name = p_killer->Get_Name();
		}
	}

	if (VictimId != PLAYER_ID_UNKNOWN) {
      p_victim = cPlayerManager::Find_Player(VictimId);
		if (p_victim != NULL) {
			victim_name = p_victim->Get_Name();
		}
	}

   /*
	if (cPlayerManager::Is_Kill_Treasonous(p_killer, p_victim)) {

		WideStringClass formatted_text;
		formatted_text.Format(
			u"%s %s %s\n", 
         killer_name,
         TRANSLATION(IDS_MP_TREASON_PHRASE), 
         victim_name);
		Get_Text_Display()->Print_Informational(formatted_text);

   } else {

		WideStringClass formatted_text;
		formatted_text.Format(
			u"%s %s %s\n", 
         killer_name,
			TRANSLATION(IDS_MP_DEFAULT_KILL_PHRASE),
         victim_name);
		Get_Text_Display()->Print_Informational(formatted_text);

		if (cNetwork::I_Am_Client() && KillerId == cNetwork::Get_My_Id() && 
			KillerId != VictimId) {
			WWAudioClass::Get_Instance()->Create_Instant_Sound("My_Kill", Matrix3D(1));
		}
   }
	*/

	if (p_killer != NULL && p_victim != NULL) {
		
		//
		//	Determine which message to display
		//
		WideStringClass message;
		if (cPlayerManager::Is_Kill_Treasonous(p_killer, p_victim)) {
			message.Format(TRANSLATION(IDS_MP_TREASON_PHRASE));
		} else {
			message.Format(TRANSLATION(IDS_MP_DEFAULT_KILL_PHRASE));

			if (cNetwork::I_Am_Client() && KillerId == cNetwork::Get_My_Id() && 
				KillerId != VictimId) {
				WWAudioClass::Get_Instance()->Create_Instant_Sound("My_Kill", Matrix3D(1));
			}
		}

		//
		//	Display the message...
		//
		WideStringClass formatted_text;
		formatted_text.Format(u"%s %s %s\n", killer_name.Peek_Buffer (), message.Peek_Buffer (), victim_name.Peek_Buffer ());
		CombatManager::Get_Message_Window ()->Add_Message (formatted_text, p_killer->Get_Color ());
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPlayerKill::Export_Creation(BitStreamClass & packet)
{
	cNetEvent::Export_Creation(packet);

	packet.Add(KillerId);
	packet.Add(VictimId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPlayerKill::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	packet.Get(KillerId);
	packet.Get(VictimId);

	Act();
}
