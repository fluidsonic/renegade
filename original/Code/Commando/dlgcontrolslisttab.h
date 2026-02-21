
#ifndef __DLGCONTROLSLISTTAB_H
#define __DLGCONTROLSLISTTAB_H

#include "childdialog.h"
#include "resource.h"
#include "vector.h"
#include "dlgmessagebox.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class InputCtrlClass;

////////////////////////////////////////////////////////////////
//
//	ControlsListTabClass
//
////////////////////////////////////////////////////////////////
class ControlsListTabClass : public ChildDialogClass, public Observer<DlgMsgBoxEvent>
{
public:
	
	////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////	
	ControlsListTabClass (int res_id);

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Reload (void);
	bool		On_InputCtrl_Get_Key_Info (InputCtrlClass *viewer_ctrl, int ctrl_id, int vkey_id, WideStringClass &key_name, int *game_key_id);

	//
	//	Configuration
	//
	void		Add_Function (int function_id, int pri_ctrl_id, int sec_ctrl_id);

	//
	//	Notifications
	//
	void		HandleNotification (DlgMsgBoxEvent &event);

protected:

	////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////
	void				Load_Key_Mappings (void);
	int				Find_Function_By_Key (int curr_function_id, int key_id);
	void				Remap_Key (int ctrl_id, int function_id, int input_id);
	void				Clear_Key (int key_id, bool clear_zoom);
	void				Prompt_User (void);
	const WCHAR *	Get_Function_Name (int function_id);

	////////////////////////////////////////////////////////////////
	//	Protected member data
	////////////////////////////////////////////////////////////////
	DynamicVectorClass<int>	FunctionIDList;
	DynamicVectorClass<int>	PrimaryCtrlIDList;
	DynamicVectorClass<int>	SecondaryCtrlIDList;

	int PendingOldFunctionID;
	int PendingCtrlID;
	int PendingFunctionID;
	int PendingDIK_ID;
};

#endif //__DLGCONTROLSLISTTAB_H

