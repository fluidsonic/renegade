#include "global.h"
#include "renegadeplayerterminal.h"
#include "dlgcncpurchasemainmenu.h"
#include "soldier.h"
#include "playertype.h"
#include "renegadedialogmgr.h"
#include "resource.h"

//////////////////////////////////////////////////////////////////////
//	Global variables
//////////////////////////////////////////////////////////////////////
static RenegadePlayerTerminalClass	_ThePlayerTerminal;

//////////////////////////////////////////////////////////////////////
//
//	RenegadePlayerTerminalClass
//
//////////////////////////////////////////////////////////////////////
RenegadePlayerTerminalClass::RenegadePlayerTerminalClass (void)
{
	_TheInstance = this;
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	~RenegadePlayerTerminalClass
//
//////////////////////////////////////////////////////////////////////
RenegadePlayerTerminalClass::~RenegadePlayerTerminalClass (void)
{
	_TheInstance = NULL;
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Show_Terminal_Dialog
//
//////////////////////////////////////////////////////////////////////
void
RenegadePlayerTerminalClass::Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM team)
{
	//
	//	Configure the team setting for the dialog
	//
	CNCPurchaseMainMenuClass *dialog = new CNCPurchaseMainMenuClass;
	dialog->Set_Team (team);

	//
	//	Show the dialog
	//
	dialog->Start_Dialog ();
	REF_PTR_RELEASE (dialog);
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Display_Terminal
//
//////////////////////////////////////////////////////////////////////
void
RenegadePlayerTerminalClass::Display_Terminal (SoldierGameObj *player, TYPE type)
{
	//if (player == NULL) {
	if (player == NULL || player->Get_Player_Data() == NULL) {
		return ;
	}
	
	//
	//	Check to ensure this player can access the given terminal
	//
	if (player->Get_Player_Type () == PLAYERTYPE_GDI && type == TYPE_GDI) {
		Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_GDI);
	} else if (player->Get_Player_Type () == PLAYERTYPE_NOD && type == TYPE_NOD) {
		Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_NOD);
	} else if (type == TYPE_MUTANT) {
		
		//
		//	Determine what player terminal to display -- GDI mutant or NOD mutant
		//
		if (player->Get_Player_Type () == PLAYERTYPE_GDI) {
			Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_MUTANT_GDI);
		} else {
			Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_MUTANT_NOD);
		}

	} else {
		
		//
		//	Display a dialog to the user telling them they don't have access to this terminal.
		//
		RenegadeDialogMgrClass::Do_Simple_Dialog (IDD_CNC_PURCHASE_ACCESS_DENIED);
	}

	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Display_Default_Terminal_For_Player
//
//////////////////////////////////////////////////////////////////////
void
RenegadePlayerTerminalClass::Display_Default_Terminal_For_Player (SoldierGameObj *player)
{
	//if (player == NULL) {
	if (player == NULL || player->Get_Player_Data() == NULL) {
		return ;
	}

	if (player->Get_Player_Type () == PLAYERTYPE_GDI) {
		Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_GDI);
	} else if (player->Get_Player_Type () == PLAYERTYPE_NOD) {
		Show_Terminal_Dialog (PurchaseSettingsDefClass::TEAM_NOD);
	}

	return ;
}
