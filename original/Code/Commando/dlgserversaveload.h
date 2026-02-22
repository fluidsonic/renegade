#pragma once

#include "global.h"

#include "menudialog.h"
#include "dlgmessagebox.h"
#include "translatedb.h"
#include "string_ids.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class ServerSettingsClass;

////////////////////////////////////////////////////////////////
//
//	ControlSaveLoadMenuClass
//
////////////////////////////////////////////////////////////////
class ServerSaveLoadMenuClass : public MenuDialogClass, public Observer<DlgMsgBoxEvent>
{
public:

	//////////////////////////////////////////////////////////////
	//	Public constructors/destructor
	//////////////////////////////////////////////////////////////
	ServerSaveLoadMenuClass (void);

	//////////////////////////////////////////////////////////////
	//	Public methods
	//////////////////////////////////////////////////////////////

	//
	//	Inherited
	//
	void		On_Init_Dialog (void);
	void		On_Command (int ctrl_id, int message_id, DWORD param);
	void		On_ListCtrl_Delete_Entry (ListCtrlClass *list_ctrl, int ctrl_id, int item_index);
	void		On_ListCtrl_Sel_Change (ListCtrlClass *list_ctrl, int	ctrl_id, int old_index, int	new_index);
	void		On_EditCtrl_Enter_Pressed (EditCtrlClass *edit_ctrl, int ctrl_id);
	void		Save_Now(void);

	static void Next_Dialog(void);

private:

	////////////////////////////////////////////////////////////////
	//	Static methods
	////////////////////////////////////////////////////////////////
	static int CALLBACK ListSortCallback (ListCtrlClass *list_ctrl, int item_index1, int item_index2, uintptr_t user_param);

	//////////////////////////////////////////////////////////////
	//	Private methods
	//////////////////////////////////////////////////////////////
	void		Delete_Config (void);
	void		Load_Config (void);
	void		Save_Config (bool prompt);
	int		Insert_Configuration (ServerSettingsClass *config);
	void		HandleNotification (DlgMsgBoxEvent &event);

	bool		YesNo;
};

#define MAX_SETTINGS_FILES 500

class ServerSettingsClass
{
	public:

		ServerSettingsClass(char *filename = "svrcfg_cnc.ini", const WCHAR *configname = TRANSLATE(IDS_SERVER_SAVELOAD_DEFAULT), int file_number = 0);
		ServerSettingsClass(ServerSettingsClass *other);
		bool Is_Default(void) {return(FileNumber == 0);}
		bool Is_Default_Custom(void) {return(FileNumber == 1);}

		StringClass			RawFileName;
		WideStringClass	ConfigName;

		int			FileNumber;
		bool			IsCustom;
};

class ServerSettingsManagerClass
{
	public:
		static void Scan(void);
		static int Get_Num_Settings_Files(void) {return(ServerSettingsList.Count());}
		static ServerSettingsClass *Get_Settings(int index);
		static void Load_Settings(ServerSettingsClass *settings);
		static void Delete_Configuration(ServerSettingsClass *settings);
		static void Save_Configuration(ServerSettingsClass *settings);
		static ServerSettingsClass *Add_Configuration(WideStringClass *display_name);

	private:
		static DynamicVectorClass<ServerSettingsClass*> ServerSettingsList;
		static void Clear_Settings_List(void);

};
