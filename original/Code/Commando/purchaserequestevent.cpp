#include "purchaserequestevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "cnetwork.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "vendor.h"
#include "playertype.h"
#include "purchaseresponseevent.h"
#include "apppackettypes.h"
#include "consolemode.h"

DECLARE_NETWORKOBJECT_FACTORY(cPurchaseRequestEvent, NETCLASSID_PURCHASEREQUESTEVENT);

//-----------------------------------------------------------------------------
cPurchaseRequestEvent::cPurchaseRequestEvent(void)
{
	SenderId			= 0;
	PurchaseType	= VendorClass::TYPE_SUPPLY;
	ItemIndex		= 0;
	AltSkinIndex	= -1;

	Set_App_Packet_Type(APPPACKETTYPE_PURCHASEREQUESTEVENT);
}

//-----------------------------------------------------------------------------
void
cPurchaseRequestEvent::Init(VendorClass::PURCHASE_TYPE type, int item_index, int alt_skin_index)
{

	SenderId			= cNetwork::Get_My_Id();
	PurchaseType	= type;
	ItemIndex		= item_index;
	AltSkinIndex	= alt_skin_index;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	if (cNetwork::I_Am_Server()) {
		Act();
	} else {
		Set_Object_Dirty_Bit(0, BIT_CREATION, true);
	}
}

//-----------------------------------------------------------------------------
void
cPurchaseRequestEvent::Act(void)
{

	//
	//	Lookup the data needed to make the purchase
	//
	SoldierGameObj *player = GameObjManager::Find_Soldier_Of_Client_ID(SenderId);

	VendorClass::PURCHASE_ERROR result = VendorClass::PERR_UNKNOWN;

	if (The_Game()->Is_Gameplay_Permitted()) 
	{
		//
		//	Attempt to make the purchase
		//
		result = VendorClass::Purchase_Item (player, (VendorClass::PURCHASE_TYPE)PurchaseType, ItemIndex, AltSkinIndex, false);
	} 
	else 
	{
		result = VendorClass::PERR_NO_FACTORY;
	}
	
	//
	// Show the server admin who bought the vehicle.
	//
	if (cNetwork::I_Am_Only_Server()) {
		if (result == VendorClass::PERR_SUCCESS && (VendorClass::PURCHASE_TYPE)PurchaseType == VendorClass::TYPE_VEHICLE) {
			cPlayer* player = cPlayerManager::Find_Player(SenderId);
			StringClass short_name(true);
			player->Get_Name().Convert_To(short_name);
			ConsoleBox.Print_Maybe("%s purchased a vehicle\n", short_name.Peek_Buffer());
		}	
	}

	//
	//	Now send the response to the client
	//
	cPurchaseResponseEvent * p_event = new cPurchaseResponseEvent;
	p_event->Init((int) result, SenderId);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPurchaseRequestEvent::Export_Creation(BitStreamClass & packet)
{

	cNetEvent::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(PurchaseType);
	packet.Add(ItemIndex);
	packet.Add(AltSkinIndex);	

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cPurchaseRequestEvent::Import_Creation(BitStreamClass & packet)
{
	cNetEvent::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(PurchaseType);
	packet.Get(ItemIndex);
	packet.Get(AltSkinIndex);	

	Act();
}