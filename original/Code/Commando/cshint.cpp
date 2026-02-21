#include "cshint.h"

#include <stdio.h>
#include <stdlib.h>

#include "debug.h"
#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "apppackettypes.h"

DECLARE_NETWORKOBJECT_FACTORY(cCsHint, NETCLASSID_CSHINT);

//-----------------------------------------------------------------------------
cCsHint::cCsHint(void)
{
	SenderId		= -1;
	SubjectId	= -1;

	Set_App_Packet_Type(APPPACKETTYPE_CSHINT);
}

//-----------------------------------------------------------------------------
void
cCsHint::Init(int subject_id)
{
	WWASSERT(subject_id >= 0);

	WWASSERT(cNetwork::I_Am_Only_Client());

	SenderId		= cNetwork::Get_My_Id();
	SubjectId	= subject_id;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);

	//WWDEBUG_SAY(("cCsHint::Init requesting hint for object id %d\n", SubjectId));
}

//-----------------------------------------------------------------------------
void
cCsHint::Act(void)
{
	WWASSERT(cNetwork::I_Am_Server());

	NetworkObjectClass * p_object = NetworkObjectMgrClass::Find_Object(SubjectId);
	if (p_object != NULL)
	{
		p_object->Increment_Client_Hint_Count(SenderId);
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsHint::Export_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Only_Client());

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(SubjectId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsHint::Import_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(SubjectId);

	Act();
}
