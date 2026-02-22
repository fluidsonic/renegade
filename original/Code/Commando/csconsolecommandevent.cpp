#include "global.h"
#include "csconsolecommandevent.h"


#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "consolefunction.h"
#include "apppackettypes.h"
#include "networkobjectmgr.h"

DECLARE_NETWORKOBJECT_FACTORY(cCsConsoleCommandEvent, NETCLASSID_CSCONSOLECOMMANDEVENT);

//-----------------------------------------------------------------------------
cCsConsoleCommandEvent::cCsConsoleCommandEvent(void)
{
	::strcpy(Command, "");

	Set_App_Packet_Type(APPPACKETTYPE_CSCONSOLECOMMANDEVENT);
}

//-----------------------------------------------------------------------------
void
cCsConsoleCommandEvent::Init(LPCSTR command)
{

	::strcpy(Command, command);

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cCsConsoleCommandEvent::Act(void)
{

	if (GameModeManager::Find("Combat")->Is_Active()) {
		ConsoleFunctionManager::Parse_Input(Command);
	}
}

//-----------------------------------------------------------------------------
void
cCsConsoleCommandEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add_Terminated_String(Command);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsConsoleCommandEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get_Terminated_String(Command, sizeof(Command));

	Act();

	Set_Delete_Pending();
}
