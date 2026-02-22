#include "purchaseresponseevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "gamemode.h"
#include "cnetwork.h"
#include "consolefunction.h"
#include "vendor.h"
#include "translatedb.h"
#include "string_ids.h"
#include "textdisplay.h"
#include "apppackettypes.h"
#include "messagewindow.h"
#include "wwaudio.h"

DECLARE_NETWORKOBJECT_FACTORY(cPurchaseResponseEvent, NETCLASSID_PURCHASERESPONSEEVENT);

//-----------------------------------------------------------------------------
cPurchaseResponseEvent::cPurchaseResponseEvent(void)
{
	PurchaserId = -1;
	ResponseId = -1;

	Set_App_Packet_Type(APPPACKETTYPE_PURCHASERESPONSEEVENT);
}

//-----------------------------------------------------------------------------
void
cPurchaseResponseEvent::Init(int response_id, int client_id)
{

	PurchaserId	= client_id;
	ResponseId = response_id;

	if (cNetwork::I_Am_Client() && PurchaserId == cNetwork::Get_My_Id())
	{
		Act();
	}
	else
	{
		Set_Object_Dirty_Bit(client_id, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cPurchaseResponseEvent::Act(void)
{
	if (!GameModeManager::Find("Combat")->Is_Active()) {
		return;
	}

	/*
	if (PurchaserId != cNetwork::Get_My_Id()) {
		return;
	}
	*/

	WideStringClass wide_string;

	if ( ResponseId == VendorClass::PERR_SUCCESS ) {
		wide_string.Format( u"%s\n", TRANSLATION(IDS_MP_CNC_PURCHASE_GRANTED) );
		//
		// Play a custom SFX for feedback
		//
		WWAudioClass::Get_Instance()->Create_Instant_Sound("Purchase_Granted", Matrix3D(1));
	} else if ( ResponseId == VendorClass::PERR_NO_FUNDS ) {
		wide_string.Format( u"%s\n", TRANSLATION(IDS_MP_CNC_INSUFFICIENT_FUNDS) );
	} else if ( ResponseId == VendorClass::PERR_NO_FACTORY ) {
		wide_string.Format( u"%s\n", TRANSLATION(IDS_MP_CNC_FACTORY_UNAVAILABLE) );
	} else if ( ResponseId == VendorClass::PERR_OPERATION_PENDING ) {
		wide_string.Format( u"%s\n", TRANSLATION(IDS_MP_CNC_TRANSACTION_PENDING) );
	} else if ( ResponseId == VendorClass::PERR_NOT_IN_STOCK ) {			
		wide_string.Format( u"%s\n", TRANSLATION(IDS_MP_CNC_NOT_IN_STOCK) );
	}

	//
	//	Display the message...
	//
	CombatManager::Get_Message_Window ()->Add_Message (wide_string, Vector3 (0.7F, 0.7F, 0.7F));		
	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPurchaseResponseEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(PurchaserId);
	packet.Add(ResponseId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPurchaseResponseEvent::Import_Creation(BitStreamClass & packet)
{

	cNetEvent::Import_Creation(packet);

	packet.Get(PurchaserId);
	packet.Get(ResponseId);

	Act();
}
