#include "global.h"
#include "cshint.h"

#include <stdio.h>
#include <stdlib.h>

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

	SenderId		= cNetwork::Get_My_Id();
	SubjectId	= subject_id;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);

	//
}

//-----------------------------------------------------------------------------
void
cCsHint::Act(void)
{

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

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(SubjectId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsHint::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(SubjectId);

	Act();
}
