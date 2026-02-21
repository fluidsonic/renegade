#include "clientbboevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "apppackettypes.h"


DECLARE_NETWORKOBJECT_FACTORY(cClientBboEvent, NETCLASSID_CLIENTBBOEVENT);

//-----------------------------------------------------------------------------
cClientBboEvent::cClientBboEvent(void)
{
	SenderId = 0;
	Bbo		= 0;

	Set_App_Packet_Type(APPPACKETTYPE_CLIENTBBOEVENT);
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Init(int bbo)
{
	WWASSERT(cNetwork::I_Am_Client());
	//WWASSERT(bbo > 0);

	SenderId = cNetwork::Get_My_Id();
	Bbo		= bbo;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Act(void)
{
   WWASSERT(cNetwork::I_Am_Server());

	cRemoteHost * p_rhost = cNetwork::Get_Server_Rhost(SenderId);
	if (p_rhost != NULL)	{
		p_rhost->Set_Maximum_Bps(Bbo);
	   WWDEBUG_SAY(("Client %d adjusted bbo to %d.\n", SenderId, Bbo));
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Export_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Only_Client());

	cNetEvent::Export_Creation(packet);

	WWASSERT(SenderId > 0);
	WWASSERT(Bbo > 0);

	packet.Add(SenderId);
	packet.Add(Bbo);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Import_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(Bbo);

	WWASSERT(SenderId > 0);
	WWASSERT(Bbo > 0);

	Act();
}
