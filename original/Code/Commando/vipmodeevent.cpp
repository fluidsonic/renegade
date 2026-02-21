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


	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);


	packet.Add(SenderId);
	packet.Add_Terminated_String((LPCSTR) Password, true);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cVipModeEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get_Terminated_String(Password.Get_Buffer(256), 256, true);


	Act();
}
