#include "global.h"
#include "clientbboevent.h"


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
	//assert(bbo > 0);

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

	cRemoteHost * p_rhost = cNetwork::Get_Server_Rhost(SenderId);
	if (p_rhost != NULL)	{
		p_rhost->Set_Maximum_Bps(Bbo);
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(Bbo);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cClientBboEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(Bbo);

	Act();
}
