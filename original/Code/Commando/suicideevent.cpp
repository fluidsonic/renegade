#include "suicideevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "apppackettypes.h"
#include "playermanager.h"
#include "sctextobj.h"
#include "translatedb.h"
#include "string_ids.h"

DECLARE_NETWORKOBJECT_FACTORY(cSuicideEvent, NETCLASSID_SUICIDEEVENT);

//-----------------------------------------------------------------------------
cSuicideEvent::cSuicideEvent(void)
{
	SenderId = 0;

	Set_App_Packet_Type(APPPACKETTYPE_SUICIDEEVENT);
}

//-----------------------------------------------------------------------------
void
cSuicideEvent::Init(void)
{

	SenderId = cNetwork::Get_My_Id();

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cSuicideEvent::Act(void)
{

	SmartGameObj * p_soldier = GameObjManager::Find_Soldier_Of_Client_ID(SenderId);
	if (p_soldier != NULL)
	{
		p_soldier->Set_Delete_Pending();
	}

	//
	// Increment Deaths
	//
	cPlayer * p_player = cPlayerManager::Find_Player(SenderId);
	if (p_player != NULL) 
	{
		//p_player->Increment_Deaths();
		p_player->Set_Money(0);
	}

	//
	// Tell everyone
	//
	WideStringClass text;
	//text.Format(u"_%s_committed_suicide_", p_player->Get_Name());
	text.Format(u"%s %s", (const WCHAR*)p_player->Get_Name(), TRANSLATE(IDS_MP_COMMITTED_SUICIDE));
	cScTextObj * p_message = new cScTextObj;
	p_message->Init(text, TEXT_MESSAGE_PUBLIC, false, HOST_TEXT_SENDER, -1);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cSuicideEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cSuicideEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);

	Act();
}
