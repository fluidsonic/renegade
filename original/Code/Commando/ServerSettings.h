#include <win.h>

class StringClass;


class ServerSettingsClass
{

	public:

		enum GameModeTypeEnum {
			MODE_NONE = 0,
			MODE_LAN
		};

		static void Set_Settings_File_Name(char *filename);
		static bool Is_Server_Settings_File_Set(void) {return((SettingsFile[0] == 0) ? false : true);}
		static bool Parse(bool apply = false);
		static bool Is_Command_Line_Mode(void) {return(IsActive);}
		static void Encrypt_Serial(StringClass serial_in, StringClass &serial_out, bool encrypt = true);
		static void Decrypt_Serial(StringClass serial_in, StringClass &serial_out);
		static const char *Get_Master_Server_Password(void) {return(MasterPassword);}
		static unsigned long Get_Master_Bandwidth(void) {return(MasterBandwidth);}
		static bool Is_Active(void) {return(IsActive);}
		static char *Get_Settings_File_Name(void) {return(SettingsFile);}
		static bool Check_Game_Settings_File(char *config_file);
		static GameModeTypeEnum Get_Game_Mode(void) {return(GameMode);}
		static int Get_Disk_Log_Size(void) {return(DiskLogSize);}

	private:
		static char SettingsFile[MAX_PATH];
		static bool IsActive;

		static char MasterPassword[128];
		static unsigned long MasterBandwidth;
		static GameModeTypeEnum GameMode;
		static int DiskLogSize;

};




