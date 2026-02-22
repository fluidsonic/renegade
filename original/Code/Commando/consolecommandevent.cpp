#include "global.h"
#include "consolecommandevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "consolefunction.h"
#include "apppackettypes.h"

DECLARE_NETWORKOBJECT_FACTORY(cConsoleCommandEvent, NETCLASSID_CONSOLECOMMANDEVENT);

//-----------------------------------------------------------------------------
cConsoleCommandEvent::cConsoleCommandEvent(void)
{
	::strcpy(Command, "");

	Set_App_Packet_Type(APPPACKETTYPE_CONSOLECOMMANDEVENT);
}

//-----------------------------------------------------------------------------
void
cConsoleCommandEvent::Init(LPCSTR command)
{

	::strcpy(Command, command);

	Set_Object_Dirty_Bit(BIT_CREATION, true);

	if (cNetwork::I_Am_Client()) {
		Act();
	}
}

//-----------------------------------------------------------------------------
void
cConsoleCommandEvent::Act(void)
{

	if (GameModeManager::Find("Combat")->Is_Active()) {
		ConsoleFunctionManager::Parse_Input(Command);
	}
}

//-----------------------------------------------------------------------------
void
cConsoleCommandEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add_Terminated_String(Command);

	//
	// TSS101001
	// Even though this object is reflected to all clients, we can set delete here because
	// TCADN is immediate and reliable.
	//
	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cConsoleCommandEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get_Terminated_String(Command, sizeof(Command));

	Act();

	Set_Delete_Pending();
}
