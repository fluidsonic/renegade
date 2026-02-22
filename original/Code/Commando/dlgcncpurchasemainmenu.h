
#ifndef __DLGCNCPURCHASEMAINMENU_H
#define __DLGCNCPURCHASEMAINMENU_H

#include "menudialog.h"
#include "purchasesettings.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class MPChatChildDialogClass;

////////////////////////////////////////////////////////////////
//
//	CNCPurchaseMainMenuClass
//
////////////////////////////////////////////////////////////////
class CNCPurchaseMainMenuClass : public MenuDialogClass
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	CNCPurchaseMainMenuClass (void);
	~CNCPurchaseMainMenuClass (void);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	
	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Destroy (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_Merchandise_Selected (MerchandiseCtrlClass *ctrl, int ctrl_id);	
	void		On_Merchandise_DblClk (MerchandiseCtrlClass *ctrl, int ctrl_id);
	bool		On_Key_Down (uint32_t key_id, uint32_t key_data);
	void		On_Frame_Update (void);	
	void		Render (void);

	//
	//	Configuration
	//
	void		Set_Team (PurchaseSettingsDefClass::TEAM team)	{ Team = team; }

	//
	// C&C Easter Eggs
	//
	static void		Enable_Secrets(bool onoff)						{ SecretsEnabled = onoff; }
	static bool		Are_Secrets_Enabled(void)						{ return SecretsEnabled; }

private:

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	void		Do_Purchase_Screen (PurchaseSettingsDefClass::TYPE type);
	void		Configure_Purchase_Controls (void);
	void		Purchase (void);
	void		Purchase_Item (int ctrl_id);
	void		Refresh_Button_States (void);
	void		Refresh_Beacon_State (void);
	void		Refresh_Message_Log (void);
	void		Clear_Selections (void);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	PurchaseSettingsDefClass::TEAM	Team;
	MPChatChildDialogClass *			ChatModule;
	float										RefreshTimer;
	int										MessageLogLength;
	static bool								SecretsEnabled;
};

#endif //__DLGCNCPURCHASEMAINMENU_H

