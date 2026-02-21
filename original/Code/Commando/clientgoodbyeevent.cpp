#include "clientgoodbyeevent.h"

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"

// 
// TSS2001 Problem - we have lost the unreliable multiblast effect for this message
//

DECLARE_NETWORKOBJECT_FACTORY(cClientGoodbyeEvent, NETCLASSID_CLIENTGOODBYEEVENT);

//-----------------------------------------------------------------------------
cClientGoodbyeEvent::cClientGoodbyeEvent(void)
{

	SenderId = -1;

	Set_App_Packet_Type(APPPACKETTYPE_CLIENTGOODBYEEVENT);
}

//-----------------------------------------------------------------------------
void
cClientGoodbyeEvent::Init(void)
{


	SenderId	= cNetwork::Get_My_Id();

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cClientGoodbyeEvent::Act(void)
{



	Set_Delete_Pending();

	//
	// TSS092101 - crashy.
	// This cleanup probably deletes *this* object...
	//
	/**/
	cNetwork::Server_Kill_Connection(SenderId);
	cNetwork::Cleanup_After_Client(SenderId);
	/**/
}

//-----------------------------------------------------------------------------
void
cClientGoodbyeEvent::Export_Creation(BitStreamClass & packet)
{


	cNetEvent::Export_Creation(packet);


	packet.Add(SenderId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cClientGoodbyeEvent::Import_Creation(BitStreamClass & packet)
{


	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);


	Act();
}
