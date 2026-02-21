#include "serverfps.h"

#include "cnetwork.h"
#include "gameobjmanager.h"
#include "apppackettypes.h"

//
// Class statics
//
cServerFps *	cServerFps::TheInstance	= NULL;

//-----------------------------------------------------------------------------
cServerFps::cServerFps(void)
{
	Set_Network_ID(NETID_SERVER_FPS);

	Fps = 0;

	Set_App_Packet_Type(APPPACKETTYPE_SERVERFPS);
}

//-----------------------------------------------------------------------------
void
cServerFps::Set_Fps(int fps)
{
	WWASSERT(fps >= 0);
	WWASSERT(cNetwork::I_Am_Server());

	Fps = fps;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_FREQUENT, true);
}

//-----------------------------------------------------------------------------
void
cServerFps::Export_Frequent(BitStreamClass &packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	packet.Add(Fps);
}

//-----------------------------------------------------------------------------
void
cServerFps::Import_Frequent(BitStreamClass &packet)
{
	WWASSERT(cNetwork::I_Am_Client());

	packet.Get(Fps);
}

//-----------------------------------------------------------------------------
void
cServerFps::Create_Instance
(
	void
)
{
	WWASSERT(TheInstance == NULL);
	TheInstance = new cServerFps;
}

//-----------------------------------------------------------------------------
void
cServerFps::Destroy_Instance
(
	void
)
{
	WWASSERT(TheInstance != NULL);
	delete TheInstance;
	TheInstance = NULL;
}

//-----------------------------------------------------------------------------
cServerFps *
cServerFps::Get_Instance
(
	void
)
{
	return TheInstance;
}



