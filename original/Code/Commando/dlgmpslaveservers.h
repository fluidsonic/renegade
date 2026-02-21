#pragma once

#ifndef _DLGMPSLAVESERVER_H
#define _DLGMPSLAVESERVER_H


#include "menudialog.h"
#include "resource.h"
#include "dialogresource.h"
#include "slavemaster.h"


class SlaveServerDialogClass : public MenuDialogClass
{
	public:

		SlaveServerDialogClass(void);

		void On_Init_Dialog(void);
		void On_Command(int ctrl_id, int mesage_id, DWORD param);
		void On_Destroy(void);
		void Load_Settings(int slavenum);
		static void Set_Slave_Settings(StringClass *file_name);
		static void Set_Slave_Button(int slavenum);

	private:

		static unsigned long EnableIDs[MAX_SLAVES];
		static unsigned long NickIDs[MAX_SLAVES];
		static unsigned long PassIDs[MAX_SLAVES];
		static unsigned long SerialIDs[MAX_SLAVES];
		static unsigned long PortIDs[MAX_SLAVES];
		static unsigned long SettingsButtons[MAX_SLAVES];

		static char ServerSettingsFileNames[MAX_SLAVES][MAX_PATH];

		static int SlaveNumber;

		static SlaveServerDialogClass *Instance;


};


#endif //_DLGMPSLAVESERVER_H