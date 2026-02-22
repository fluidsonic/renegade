#include "clientfps.h"

#include "networkobjectfactory.h"
#include "networkobjectmgr.h"
#include "cnetwork.h"
#include "playermanager.h"
#include "apppackettypes.h"

CClientFps *		PClientFps = NULL;

DECLARE_NETWORKOBJECT_FACTORY(CClientFps, NETCLASSID_CLIENTFPS);

//
// TSS2001 problem: destruction of this object on the server. Quitting and rejoining 
// a game will crash the server.
//

//-----------------------------------------------------------------------------
CClientFps::CClientFps(void)
{
	ClientId	= -1;
	Fps		= 0;

	Set_App_Packet_Type(APPPACKETTYPE_CLIENTFPS);
}

//-----------------------------------------------------------------------------
CClientFps::~CClientFps(void)
{
}

//-----------------------------------------------------------------------------
void
CClientFps::Init(void)
{

	ClientId = cNetwork::Get_My_Id();

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, NetworkObjectClass::BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
CClientFps::Set_Fps(int fps)
{

	Fps = (BYTE) fps;

	Set_Object_Dirty_Bit(0, NetworkObjectClass::BIT_FREQUENT, true);
}

//-----------------------------------------------------------------------------
void
CClientFps::Act(void)
{

	cPlayer * p_player = cPlayerManager::Find_Player(ClientId);
	if (p_player != NULL)
	{
		p_player->Set_Fps(Fps);
	}
}

//-----------------------------------------------------------------------------
void
CClientFps::Export_Creation(BitStreamClass & packet)
{

	NetworkObjectClass::Export_Creation(packet);

	packet.Add(ClientId);
}

//-----------------------------------------------------------------------------
void
CClientFps::Import_Creation(BitStreamClass & packet)
{

	NetworkObjectClass::Import_Creation(packet);

	packet.Get(ClientId);
}

//-----------------------------------------------------------------------------
void
CClientFps::Export_Frequent(BitStreamClass & packet)
{

	packet.Add(Fps);
}

//-----------------------------------------------------------------------------
void
CClientFps::Import_Frequent(BitStreamClass & packet)
{

	packet.Get(Fps);

	Act();
}

