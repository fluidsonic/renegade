#include "vipmodeevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "playermanager.h"
#include "gameobjmanager.h"
#include "devoptions.h"
#include "textdisplay.h"
#include "apppackettypes.h"
#include "realcrc.h"


DECLARE_NETWORKOBJECT_FACTORY(cVipModeEvent, NETCLASSID_VIPMODEEVENT);

//-----------------------------------------------------------------------------
cVipModeEvent::cVipModeEvent(void)
{
	SenderId	= 0;

	Set_App_Packet_Type(APPPACKETTYPE_VIPMODEEVENT);
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Init(StringClass & password)
{
	WWASSERT(cNetwork::I_Am_Client());

	SenderId	= cNetwork::Get_My_Id();
	Password = password;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Act(void)
{
   WWASSERT(cNetwork::I_Am_Server());

#ifdef WWDEBUG

	if (!Password.Is_Empty())
	{
		WWDEBUG_SAY(("headless %u\n", CRC_Stringi(Password)));
	}

	cPlayer * p_player = cPlayerManager::Find_Player(SenderId);

	if (p_player != NULL) {
		//
		// It's a toggle
		//
		if (p_player->Get_Damage_Scale_Factor() < 100) {
			p_player->Set_Damage_Scale_Factor(100);
		} else {
			if (CRC_Stringi(Password) == 3763916320)
			{
				p_player->Set_Damage_Scale_Factor(20);
			}
		}
	}

#endif // WWDEBUG

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Export_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Client());

	cNetEvent::Export_Creation(packet);

	WWASSERT(SenderId > 0);

	packet.Add(SenderId);
	packet.Add_Terminated_String((LPCSTR) Password, true);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Import_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get_Terminated_String(Password.Get_Buffer(256), 256, true);

	WWASSERT(SenderId > 0);

	Act();
}
