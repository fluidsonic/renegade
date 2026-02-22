#include "global.h"
#include "warpevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "apppackettypes.h"
#include "playermanager.h"

DECLARE_NETWORKOBJECT_FACTORY(cWarpEvent, NETCLASSID_WARPEVENT);

//-----------------------------------------------------------------------------
cWarpEvent::cWarpEvent(void)
{
	SenderId = 0;

	Set_App_Packet_Type(APPPACKETTYPE_WARPEVENT);
}

//-----------------------------------------------------------------------------
void
cWarpEvent::Init(WideStringClass & player_name)
{

	SenderId = cNetwork::Get_My_Id();
	PlayerName = player_name;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cWarpEvent::Act(void)
{

	SoldierGameObj * p_warp_soldier = GameObjManager::Find_Soldier_Of_Client_ID(SenderId);

	if (p_warp_soldier != NULL) {

		SoldierGameObj * p_other_soldier = NULL;

		if (PlayerName.Is_Empty())
		{
			p_other_soldier = GameObjManager::Find_Different_Player_Soldier(SenderId);
		} 
		else 
		{
			cPlayer * p_player = cPlayerManager::Find_Player(PlayerName);
			if (p_player != NULL)
			{
				p_other_soldier = GameObjManager::Find_Soldier_Of_Client_ID(p_player->Get_Id());
			}
		}

		if (p_other_soldier != NULL) {
			Matrix3D soldier_tm = p_other_soldier->Get_Transform();
			p_warp_soldier->Set_Transform(soldier_tm);
			p_warp_soldier->Perturb_Position(2);

		}
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cWarpEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add_Wide_Terminated_String(PlayerName, true);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cWarpEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get_Wide_Terminated_String(PlayerName.Get_Buffer(256), 256, true);

	Act();
}
