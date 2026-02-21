#include "cspingrequestevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "debug.h"
#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"
#include "scpingresponseevent.h"
#include "wwprofile.h"


DECLARE_NETWORKOBJECT_FACTORY(cCsPingRequestEvent, NETCLASSID_CSPINGREQUESTEVENT);

//-----------------------------------------------------------------------------
cCsPingRequestEvent::cCsPingRequestEvent(void)
{
	SenderId		= -1;
	PingNumber	= -1;

	Set_App_Packet_Type(APPPACKETTYPE_CSPINGREQUESTEVENT);
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Init(int ping_number)
{
	//WWDEBUG_SAY(("cCsPingRequestEvent::Init at frame %d\n", WWProfileManager::Get_Frame_Count_Since_Reset()));

	WWASSERT(ping_number >= 0);

	WWASSERT(cNetwork::I_Am_Only_Client());

	SenderId		= cNetwork::Get_My_Id();
	PingNumber	= ping_number;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Act(void)
{
	WWASSERT(cNetwork::I_Am_Server());

	cScPingResponseEvent * p_event = new cScPingResponseEvent;
	p_event->Init(SenderId, PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Export_Creation(BitStreamClass & packet)
{
	//WWDEBUG_SAY(("cCsPingRequestEvent::Export at frame %d\n", WWProfileManager::Get_Frame_Count_Since_Reset()));

	WWASSERT(cNetwork::I_Am_Only_Client());

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Import_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(PingNumber);

	Act();
}
