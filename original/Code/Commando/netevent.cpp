#include "netevent.h"

#include "cnetwork.h"

//-----------------------------------------------------------------------------
void
cNetEvent::Init(void)
{

	if (cNetwork::I_Am_Client())
	{
		Act();
	}

	//
	// This is a transient object. It is created, rendered, and destroyed immediately.
	//
	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cNetEvent::Export_Creation(BitStreamClass &packet)
{
	NetworkObjectClass::Export_Creation(packet);
}

//-----------------------------------------------------------------------------
void
cNetEvent::Import_Creation(BitStreamClass &packet)
{
	NetworkObjectClass::Import_Creation(packet);
}




