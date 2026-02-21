#include "evictionevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "translatedb.h"
#include "string_ids.h"
#include "textdisplay.h"
#include "dialogtests.h"
#include "apppackettypes.h"
#include "messagewindow.h"

// 
// TSS2001 Problem - we have lost the unreliable multiblast effect for this message
//

DECLARE_NETWORKOBJECT_FACTORY(cEvictionEvent, NETCLASSID_EVICTIONEVENT);

//-----------------------------------------------------------------------------
cEvictionEvent::cEvictionEvent(void)
{
	Set_App_Packet_Type(APPPACKETTYPE_EVICTIONEVENT);
}

//-----------------------------------------------------------------------------
void
cEvictionEvent::Init(int client_id, EvictionCodeEnum code)
{
	WWASSERT(cNetwork::I_Am_Server());
	WWASSERT(client_id >= 0);

	EvictionCode = code;

	Set_Object_Dirty_Bit(client_id, BIT_CREATION, true);

	//
	// This is a transient object. It is created, rendered, and destroyed immediately.
	//
	//Set_Delete_Pending ();
}

//-----------------------------------------------------------------------------
void
cEvictionEvent::Act(void)
{
	WWASSERT(cNetwork::I_Am_Only_Client());

   WideStringClass code_string;
	switch (EvictionCode)
	{
		case EVICTION_POOR_BANDWIDTH:
			code_string = TRANSLATION(IDS_MP_BANDWIDTH_INSUFFICIENT);
			break;

		default:
			DIE;
	}

	WideStringClass widestring;
	widestring.Format(
		L"%s: %s", 
		TRANSLATION(IDS_MP_YOU_ARE_EVICTED), 
		code_string);
   WWASSERT(CombatManager::Get_Message_Window() != NULL);

	//
	//	Display the message...
	//
	CombatManager::Get_Message_Window ()->Add_Message (widestring);
	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cEvictionEvent::Export_Creation(BitStreamClass & packet)
{
	WWASSERT(cNetwork::I_Am_Server());

	cNetEvent::Export_Creation(packet);

	packet.Add((int) EvictionCode);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cEvictionEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	WWASSERT(cNetwork::I_Am_Only_Client());

	int eviction_code = packet.Get(eviction_code);
	EvictionCode = (EvictionCodeEnum) eviction_code;

	Act();
}






