#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __VENDOR_H
#define __VENDOR_H

#include "building.h"
#include "basecontroller.h"
#include "characterclasssettings.h"


////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class SoldierGameObj;
class PurchaseSettingsDefClass;


////////////////////////////////////////////////////////////////
//
//	VendorClass
//
////////////////////////////////////////////////////////////////
class VendorClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constants
	////////////////////////////////////////////////////////////////
	typedef enum
	{
		PERR_UNKNOWN				= -1,
		PERR_SUCCESS				= 0,
		PERR_OPERATION_PENDING,
		PERR_NO_FUNDS,
		PERR_NO_FACTORY,
		PERR_NOT_IN_STOCK

	}	PURCHASE_ERROR;

	typedef enum
	{
		TYPE_CHARACTER				= 0,
		TYPE_VEHICLE,
		TYPE_ENLISTED_CHARACTER,
		TYPE_BEACON,
		TYPE_SUPPLY,
		TYPE_SECRET_CHARACTER,
		TYPE_SECRET_VEHICLE,

	}	PURCHASE_TYPE;
	
	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Purchase support
	//
	static PURCHASE_ERROR	Purchase_Item (SoldierGameObj *player, PURCHASE_TYPE type, int item_index, int alt_skin_index = -1, bool is_from_server = true);

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	static PURCHASE_ERROR	Purchase_Vehicle (BaseControllerClass *base, SoldierGameObj *player, int cost, int definition_id);
	static PURCHASE_ERROR	Purchase_Powerup (BaseControllerClass *base, SoldierGameObj *player, int cost, int definition_id);
	static PURCHASE_ERROR	Purchase_Character (BaseControllerClass *base, SoldierGameObj *player, int cost, int definition_id);
	
	static void					Get_Merchandise_Information (SoldierGameObj *player, PURCHASE_TYPE type, int item_index, int alt_skin_index, int &cost, int &definition);
	static void					Grant_Supplies (SoldierGameObj *player);
};


#endif //__VENDOR_H

