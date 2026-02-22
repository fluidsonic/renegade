#pragma once

#include "global.h"

#include "menudialog.h"
#include "playerterminal.h"
#include "purchasesettings.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class PurchaseSettingsDefClass;
class MerchandiseCtrlClass;

////////////////////////////////////////////////////////////////
//
//	CNCPurchaseMenuClass
//
////////////////////////////////////////////////////////////////
class CNCPurchaseMenuClass : public MenuDialogClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	CNCPurchaseMenuClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	
	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Frame_Update (void);
	void		On_Merchandise_Selected (MerchandiseCtrlClass *ctrl, int ctrl_id);
	void		On_Merchandise_DblClk (MerchandiseCtrlClass *ctrl, int ctrl_id);
	bool		On_Key_Down (uint32_t key_id, uint32_t key_data);
	void		Render (void);

	//
	//	Configuration
	//
	void		Set_Definition (PurchaseSettingsDefClass *definition)	{ Definition = definition; }
	void		Set_Team (PlayerTerminalClass::TYPE team)					{ Team = team; }
	void		Set_Type (PurchaseSettingsDefClass::TYPE type)			{ PurchaseType = type; }

	void		Enable_Production (bool onoff)	{ IsProductionDisabled = !onoff; }
	bool		Is_Production_Enabled (void)		{ return IsProductionDisabled; }

	void		Set_Cost_Scaling_Factor (float factor)	{ CostScalingFactor = factor; }
	float		Get_Cost_Scaling_Factor (void) const	{ return CostScalingFactor; }

	void		Enable_Multiple_Purchases (bool onoff)			{ EnableMultiplePurchases = onoff; }
	bool		Are_Multiple_Purchases_Enabled (void) const	{ return EnableMultiplePurchases; }

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////	
	void		Add_Item_To_Shopping_Cart (int ctrl_index);
	void		Clear_Shopping_Cart (void);
	void		Purchase (void);
	void		Purchase_Item (int ctrl_id);
	bool		Is_Definition_OK (DefinitionClass *definition);
	bool		Verify_Vehicle_Purchase (void);

	void		Update_Enabled_Status (void);
	void		Update_Building_Health (void);
	void		Configure_Building_Icons (void);

	////////////////////////////////////////////////////////////////
	//	Private structs
	////////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	PlayerTerminalClass::TYPE				Team;
	PurchaseSettingsDefClass::TYPE		PurchaseType;
	PurchaseSettingsDefClass *				Definition;
	DynamicVectorClass<int>					ShoppingList;
	int											TotalCost;
	bool											IsProductionDisabled;
	bool											EnableMultiplePurchases;
	float											CostScalingFactor;
	float											HealthUpdateTimer;
	float											EnabledStateUpdateTimer;
};
