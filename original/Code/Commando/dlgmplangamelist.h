
#ifndef __DLG_MP_LAN_GAME_LIST_H
#define __DLG_MP_LAN_GAME_LIST_H

#include "menudialog.h"
#include "resource.h"
#include <Signaler.h>

class DlgPasswordPrompt;

////////////////////////////////////////////////////////////////
//
//	MPLanGameListMenuClass
//
////////////////////////////////////////////////////////////////
class MPLanGameListMenuClass : public MenuDialogClass,
	protected Signaler<DlgPasswordPrompt>
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	MPLanGameListMenuClass (void);
	~MPLanGameListMenuClass();

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////
	void		On_Init_Dialog (void);
	void		On_Destroy (void);
	void		On_Command (int ctrl_id, int mesage_id, DWORD param);
	bool		On_Key_Down(uint32 key_id, uint32 key_data);
	
	static void	Set_Update_Nickname(void)						{ UpdateNickname = true; }

	
	void		On_ListCtrl_Delete_Entry (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_DblClk (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_EditCtrl_Change(EditCtrlClass* edit, int id);

	//
	//	Singleton access
	//
	static void								Display (void);
	static MPLanGameListMenuClass *	Get_Instance (void)	{ return _TheInstance; }

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////

	//
	//	Inherited
	//
	void		On_Last_Menu_Ending (void);
	void		On_Frame_Update (void);

	void		Update_Game_List (void);
	void		Join_Game (void);
	void		ReceiveSignal(DlgPasswordPrompt&);
	void		Connect_To_Server (void);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	int										UpdateTimer;
	static bool								UpdateNickname;

	static MPLanGameListMenuClass *	_TheInstance;

};

#endif //__DLG_MP_LAN_GAME_LIST_H

