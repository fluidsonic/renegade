#include "gamedataupdateevent.h"

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "apppackettypes.h"

DECLARE_NETWORKOBJECT_FACTORY(cGameDataUpdateEvent, NETCLASSID_GAMEDATAUPDATEEVENT);

//-----------------------------------------------------------------------------
cGameDataUpdateEvent::cGameDataUpdateEvent(void)
{
	Set_App_Packet_Type(APPPACKETTYPE_GAMEDATAUPDATEEVENT);
	TimeRemainingSeconds = 0;
	//ServerIsGameplayPermitted = true;
}

//-----------------------------------------------------------------------------
void
cGameDataUpdateEvent::Init(int client_id)
{
	//WWASSERT(client_id >= 0);

	// WAS: cGameDataUpdateEvent::cGameDataUpdateEvent(); (illegal qualified constructor call)

	TimeRemainingSeconds = (int) The_Game()->Get_Time_Remaining_Seconds();
	//ServerIsGameplayPermitted = The_Game()->Get_Server_Is_Gameplay_Permitted();
	HostedGameNumber = The_Game()->Get_Hosted_Game_Number();

	if (client_id == -1) {
		Set_Object_Dirty_Bit(BIT_CREATION, true);
	} else {
		Set_Object_Dirty_Bit(client_id, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cGameDataUpdateEvent::Act(void)
{

	if (The_Game() != NULL && TimeRemainingSeconds > 0)
	{
		The_Game()->Set_Time_Remaining_Seconds(TimeRemainingSeconds);
		//The_Game()->Set_Server_Is_Gameplay_Permitted(ServerIsGameplayPermitted);
	}
	The_Game()->Set_Hosted_Game_Number(HostedGameNumber);

	//Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cGameDataUpdateEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(TimeRemainingSeconds);
	//packet.Add(ServerIsGameplayPermitted);
	packet.Add(HostedGameNumber);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cGameDataUpdateEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(TimeRemainingSeconds);
	//packet.Get(ServerIsGameplayPermitted);
	packet.Get(HostedGameNumber);

	Act();

	Set_Delete_Pending();
}




