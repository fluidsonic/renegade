#include "global.h"
#include "cspingrequestevent.h"


#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"
#include "scpingresponseevent.h"

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
	//

	SenderId		= cNetwork::Get_My_Id();
	PingNumber	= ping_number;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Act(void)
{

	cScPingResponseEvent * p_event = new cScPingResponseEvent;
	p_event->Init(SenderId, PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Export_Creation(BitStreamClass & packet)
{
	//

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(PingNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsPingRequestEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(PingNumber);

	Act();
}
