#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLGLOADSPGAME_H
#define __DLGLOADSPGAME_H


#include "menudialog.h"
#include "resource.h"
#include "dlgmessagebox.h"


////////////////////////////////////////////////////////////////
//
//	LoadSPGameMenuClass
//
////////////////////////////////////////////////////////////////
class LoadSPGameMenuClass : public MenuDialogClass, public Observer<DlgMsgBoxEvent>
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	LoadSPGameMenuClass (void)	:
		CurrSortCol (1),
		IsSortAscending (false),
		MenuDialogClass (IDD_MENU_LOAD_SP_GAME)	{}


	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_ListCtrl_Delete_Entry (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_Column_Click (ListCtrlClass *list_ctrl, int ctrl_id, int col_index);
	void		On_ListCtrl_DblClk (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_Sel_Change (ListCtrlClass *list_ctrl, int ctrl_id, int old_index, int new_index);

	void		Update_Button_State (void);

	////////////////////////////////////////////////////////////////
	//	Static methods
	////////////////////////////////////////////////////////////////
	static	void	Set_Game_Rank(const char * filename, int rank);

	//
	//	Singleton access
	//
	static void							Display (void);
	static LoadSPGameMenuClass *	Get_Instance (void)	{ return _TheInstance; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void		HandleNotification (DlgMsgBoxEvent &event);
	void		On_Last_Menu_Ending (void);

private:

	////////////////////////////////////////////////////////////////
	//	Static members
	////////////////////////////////////////////////////////////////
	static int CALLBACK LoadListSortCallback (ListCtrlClass *list_ctrl, int item_index1, int item_index2, uint32 user_param);

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	void		Load_Game (void);
	void		Delete_Game (bool prompt);
	int			Build_List (const char *search_string, int start_index);

	static	bool	Is_Game_Allowed(const char * filename);
	static	int		Get_Game_Rank(const char * filename);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	uint16	CurrSortCol;
	bool		IsSortAscending;

	static LoadSPGameMenuClass *_TheInstance;
};


#endif //__DLGLOADSPGAME_H

