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

	Fps = fps;

	Set_Object_Dirty_Bit(NetworkObjectClass::BIT_FREQUENT, true);
}

//-----------------------------------------------------------------------------
void
cServerFps::Export_Frequent(BitStreamClass &packet)
{

	packet.Add(Fps);
}

//-----------------------------------------------------------------------------
void
cServerFps::Import_Frequent(BitStreamClass &packet)
{

	packet.Get(Fps);
}

//-----------------------------------------------------------------------------
void
cServerFps::Create_Instance
(
	void
)
{
	TheInstance = new cServerFps;
}

//-----------------------------------------------------------------------------
void
cServerFps::Destroy_Instance
(
	void
)
{
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

