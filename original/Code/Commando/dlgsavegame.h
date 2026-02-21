#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DLG_SAVE_GAME_H
#define __DLG_SAVE_GAME_H


#include "menudialog.h"
#include "resource.h"
#include "dlgmessagebox.h"


////////////////////////////////////////////////////////////////
//
//	SaveGameMenuClass
//
////////////////////////////////////////////////////////////////
class SaveGameMenuClass : public MenuDialogClass, public Observer<DlgMsgBoxEvent>
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	SaveGameMenuClass (void)	:
		CurrSortCol (1),
		IsSortAscending (false),
		MenuDialogClass (IDD_MENU_SAVE_GAME)	{}


	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	void		On_ListCtrl_Delete_Entry (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_Column_Click (ListCtrlClass *list_ctrl, int ctrl_id, int col_index);
	void		On_ListCtrl_DblClk (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_Sel_Change (ListCtrlClass *list_ctrl, int ctrl_id, int old_index, int new_index);
	void		On_EditCtrl_Enter_Pressed (EditCtrlClass *edit_ctrl, int ctrl_id);
	void		On_EditCtrl_Change (EditCtrlClass *edit_ctrl, int ctrl_id);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void		HandleNotification (DlgMsgBoxEvent &event);
	bool		Check_HD_Space (void);
	void		Update_Button_State (void);

private:

	////////////////////////////////////////////////////////////////
	//	Static members
	////////////////////////////////////////////////////////////////
	static int CALLBACK LoadListSortCallback (ListCtrlClass *list_ctrl, int item_index1, int item_index2, uint32 user_param);

	////////////////////////////////////////////////////////////////
	//	Private methods
	////////////////////////////////////////////////////////////////
	void		Save_Game (bool prompt);
	void		Delete_Game (bool prompt);
	void		Get_Unique_Save_Filename (StringClass &filename);
	void		Update_Text_Field (void);
	void		Reload_List (const char *current_filename);

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////
	uint16	CurrSortCol;
	bool		IsSortAscending;
};


#endif //__DLG_SAVE_GAME_H

