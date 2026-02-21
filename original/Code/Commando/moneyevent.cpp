#include "moneyevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "playermanager.h"
#include "apppackettypes.h"


DECLARE_NETWORKOBJECT_FACTORY(cMoneyEvent, NETCLASSID_MONEYEVENT);

//-----------------------------------------------------------------------------
cMoneyEvent::cMoneyEvent(void)
{
	SenderId = 0;
	Amount = 0;

	Set_App_Packet_Type(APPPACKETTYPE_MONEYEVENT);
}

//-----------------------------------------------------------------------------
void
cMoneyEvent::Init(int amount)
{

	SenderId = cNetwork::Get_My_Id();
	Amount = amount;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cMoneyEvent::Act(void)
{

	cPlayer * p_player = cPlayerManager::Find_Player(SenderId);

	if (p_player != NULL && p_player->Invulnerable.Is_True()) {

		p_player->Set_Money(Amount);

	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cMoneyEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);


	packet.Add(SenderId);
	packet.Add(Amount);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cMoneyEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);


	packet.Get(SenderId);
	packet.Get(Amount);


	Act();
}





