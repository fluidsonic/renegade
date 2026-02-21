#include "requestkillevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"


DECLARE_NETWORKOBJECT_FACTORY(cRequestKillEvent, NETCLASSID_REQUESTKILLEVENT);

//-----------------------------------------------------------------------------
cRequestKillEvent::cRequestKillEvent(void)
{
	ObjectId = 0;

	Set_App_Packet_Type(APPPACKETTYPE_REQUESTKILLEVENT);
}

//-----------------------------------------------------------------------------
void
cRequestKillEvent::Init(int object_id)
{

	ObjectId = object_id;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cRequestKillEvent::Act(void)
{

	NetworkObjectClass *	p_object = NetworkObjectMgrClass::Find_Object(ObjectId);
	if (p_object != NULL) {
		p_object->Set_Delete_Pending();
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cRequestKillEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(ObjectId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cRequestKillEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);


	packet.Get(ObjectId);

	Act();
}
