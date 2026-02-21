#include "scpingresponseevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "debug.h"
#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"
#include "clientpingmanager.h"


DECLARE_NETWORKOBJECT_FACTORY(cScPingResponseEvent, NETCLASSID_SCPINGRESPONSEEVENT);

//-----------------------------------------------------------------------------
cScPingResponseEvent::cScPingResponseEvent(void)
{
	PingNumber	= -1;

	Set_App_Packet_Type(APPPACKETTYPE_SCPINGRESPONSEEVENT);
}

//-----------------------------------------------------------------------------
void
cScPingResponseEvent::Init(int sender_id, int ping_number)
{
	//WWDEBUG_SAY(("cScPingResponseEvent::Init at frame %d\n", WWProfileManager::Get_Frame_Count_Since_Reset()));



	PingNumber	= ping_number;

	Set_Object_Dirty_Bit(sender_id, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cScPingResponseEvent::Act(void)
{

	cClientPingManager::Response_Received(PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScPingResponseEvent::Export_Creation(BitStreamClass & packet)
{
	//WWDEBUG_SAY(("cScPingResponseEvent::Export_Creation at frame %d\n", WWProfileManager::Get_Frame_Count_Since_Reset()));


	cNetEvent::Export_Creation(packet);

	packet.Add(PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScPingResponseEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(PingNumber);

	Act();
}
