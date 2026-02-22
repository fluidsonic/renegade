#include "global.h"
#include "changeteamevent.h"


#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "playertype.h"
#include "playermanager.h"
#include "gamemode.h"
#include "gamedata.h"
#include "gameobjmanager.h"
#include "spawn.h"
#include "apppackettypes.h"
#include "sctextobj.h"
#include "translatedb.h"
#include "string_ids.h"
#include "c4.h"
#include "beacongameobj.h"
#include "weaponview.h"

DECLARE_NETWORKOBJECT_FACTORY(cChangeTeamEvent, NETCLASSID_CHANGETEAMEVENT);

//-----------------------------------------------------------------------------
cChangeTeamEvent::cChangeTeamEvent(void)
{
	SenderId = 0;

	Set_App_Packet_Type(APPPACKETTYPE_CHANGETEAMEVENT);
}

//-----------------------------------------------------------------------------
void
cChangeTeamEvent::Init(void)
{

	SenderId = cNetwork::Get_My_Id();

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cChangeTeamEvent::Act(void)
{

	cPlayer * p_player = cPlayerManager::Find_Player(SenderId);

	if (p_player != NULL && 
		 The_Game() != NULL && 
		 //The_Game()->Is_Team_Game() && 
		 (The_Game()->IsTeamChangingAllowed.Is_True() || p_player->Invulnerable.Is_True()))
	{
		int team = p_player->Get_Player_Type();

		int new_team = PLAYERTYPE_NOD;
		
		if (team == PLAYERTYPE_NOD) 
		{
			new_team = PLAYERTYPE_GDI;
		} 
		else 
		{
			new_team = PLAYERTYPE_NOD;
		}

		p_player->Set_Player_Type(new_team);

		//
		//	Only reset the player's cash if they've changed teams after 60 seconds
		// have elapsed.
		//
		DWORD playing_time = (TIMEGETTIME() - p_player->Get_Join_Time ());
		if (playing_time > 30000) {
			p_player->Set_Score(0);
			p_player->Set_Money(0);
		}

		SoldierGameObj * p_soldier = GameObjManager::Find_Soldier_Of_Client_ID(SenderId);
		if (p_soldier != NULL)
		{

			if ( COMBAT_STAR == p_soldier ) {
				WeaponViewClass::Reset();
			}

			// Defuse all C4 belonging to this guy
			SLNode<BaseGameObj> *objnode;
			for (	objnode = GameObjManager::Get_Game_Obj_List()->Head(); objnode; objnode = objnode->Next()) {
				PhysicalGameObj *obj = objnode->Data()->As_PhysicalGameObj();
				if ( obj && obj->As_C4GameObj() ) {
					if ( obj->As_C4GameObj()->Get_Owner() == p_soldier ) {
						obj->As_C4GameObj()->Defuse();
					}
				}
				if ( obj && obj->As_BeaconGameObj() ) {
					if ( obj->As_BeaconGameObj()->Get_Owner() == p_soldier ) {
						// disarm C4
						OffenseObjectClass unused (0.0F, 0);
						obj->As_BeaconGameObj()->Completely_Damaged( unused );
					}
				}
			}

			//
			// We have to respawn him and possibly change his model... 
			// let's just destroy the soldier and leave the rest up to God.
			//
			p_soldier->Set_Delete_Pending();
		}

		//
		// Tell everyone
		//
		WideStringClass text;
		//text.Format(u"_%s_changed_teams!_", p_player->Get_Name());
		text.Format(u"%s %s", (const WCHAR*)p_player->Get_Name(), TRANSLATE(IDS_MP_CHANGED_TEAMS));

		cScTextObj * p_message = new cScTextObj;
		p_message->Init(text, TEXT_MESSAGE_PUBLIC, false, HOST_TEXT_SENDER, -1);
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cChangeTeamEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cChangeTeamEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);

	Act();
}

//-----------------------------------------------------------------------------
bool 
cChangeTeamEvent::Is_Change_Team_Possible(void)
{
	return
		cNetwork::I_Am_Client() &&
		GameModeManager::Find("Combat") != NULL &&
		GameModeManager::Find("Combat")->Is_Active() &&
		The_Game() != NULL && 
		//The_Game()->Is_Team_Game() && 
		The_Game()->IsTeamChangingAllowed.Is_True();
}
