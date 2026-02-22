#include "ServerSettings.h"
#include "gamedata.h"
#include "gdcnc.h"
#include "ini.h"
#include "registry.h"
#include "rawfile.h"
#include "consolemode.h"
#include "_globals.h"
#include "bandwidth.h"
#include "mpsettingsmgr.h"
#include "useroptions.h"
#include "servercontrol.h"
#include "gamesideservercontrol.h"
#include "bandwidthcheck.h"

const char *ConfigSettingsName = "Config";
const char *MasterServerSection = "Server";

#define ENCRYPTION_STRING_LENGTH				128

char ServerSettingsClass::SettingsFile[MAX_PATH];
bool ServerSettingsClass::IsActive = false;
char ServerSettingsClass::MasterPassword[128];
ServerSettingsClass::GameModeTypeEnum ServerSettingsClass::GameMode = MODE_NONE;
unsigned long ServerSettingsClass::MasterBandwidth = 0;
int ServerSettingsClass::DiskLogSize = -1;

/***********************************************************************************************
 * ServerSettingsClass::Set_Settings_File_Name -- Set the name of the settings file            *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    File name                                                                         *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/1/2002 12:07PM ST : Created                                                             *
 *=============================================================================================*/
void ServerSettingsClass::Set_Settings_File_Name(char *filename)
{
	if (strlen(filename) < sizeof(SettingsFile)) {
		strcpy(SettingsFile, filename);
		IsActive = true;
	}
}

/***********************************************************************************************
 * ServerSettingsClass::Parse -- Pull the server info out of the settings file                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    True if we should apply the settings. False to parse for errors only.             *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/1/2002 12:07PM ST : Created                                                             *
 *=============================================================================================*/
bool ServerSettingsClass::Parse(bool apply)
{
	char master_settings[MAX_PATH];
	char master_nick[128];
	char master_serial[128];
	int master_port;
	int master_bw;
	char master_pass[128];
	char remote_admin_pass[128];
	char game_type[32];
	char heartbeat_list[512];
	char remote_admin_ip[128];

	MasterPassword[0] = 0;

	/*
	** IsActive is set when the settigns file name is set.
	*/
	if (IsActive) {

		if (apply) {
			ConsoleBox.Print("Applying server settings\n");
		}

		/*
		** Make sure the server config file is there. It should be since it's verified at command line parsing time.
		*/
		RawFileClass file(SettingsFile);
		if (!file.Is_Available()) {
			ConsoleBox.Print("Error - server startup file '%s' not found - aborting\n", SettingsFile);
			ConsoleBox.Wait_For_Keypress();
			return(false);
		}

		/*
		** Get the name of the master server settings file.
		*/
		INIClass ini(file);
		ini.Get_String(MasterServerSection, ConfigSettingsName, "", master_settings, sizeof(master_settings));
		if (strlen(master_settings) == 0) {
			if (apply) {
				ConsoleBox.Print("No master server settings specified - using defaults\n");
			}
		}

		/*
		** Load the master server settings from the ini file.
		*/

		/*
		** Game Type.
		*/
		GameMode = MODE_LAN;

		/*
		** Make sure the master server settings file is there.
		*/
		char filename[MAX_PATH];
		sprintf(filename, "data/%s", master_settings);
		file.Set_Name(filename);
		if (!file.Is_Available()) {
			ConsoleBox.Print("Error - server settings file '%s' not found - aborting\n", filename);
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		} else {
			if (!Check_Game_Settings_File(master_settings)) {
				ConsoleBox.Print("Error - server settings file '%s' contains errors - aborting\n", master_settings);
				ConsoleBox.Wait_For_Keypress();;
				return(false);
			}
			if (apply && The_Game()) {
				The_Game()->Set_Ini_Filename(master_settings);
			}
		}

		/*
		** Restart Flag
		*/

		RegistryClass restart_reg(APPLICATION_SUB_KEY_NAME_MP_SETTINGS);
		if (restart_reg.Is_Valid ()) {
			restart_reg.Set_Int("AutoRestartFlag", 1);
			restart_reg.Set_Int("AutoRestartType", 0);
		}

		/*
		** Nickname.
		*/
		ini.Get_String(MasterServerSection, "Nickname", "", master_nick, sizeof(master_nick));
// FDS: server settings were auto-applied without UI prompts

		/*
		** Password.
		*/
		ini.Get_String(MasterServerSection, "Password", "", master_pass, sizeof(master_pass));
// FDS: server settings were auto-applied without UI prompts

		/*
		** Serial number.
		*/
		ini.Get_String(MasterServerSection, "Serial", "", master_serial, sizeof(master_serial));
// FDS: server settings were auto-applied without UI prompts

		/*
		** Get the port number.
		*/
		master_port = ini.Get_Int(MasterServerSection, "Port", 0xffffffff);
		if (master_port != 0xffffffff && (master_port < 0 || master_port > 0xffff)) {
			ConsoleBox.Print("Error - Invalid port number %d specified for master server - aborting\n", master_port);
			ConsoleBox.Wait_For_Keypress();
			return(false);
		}

		/*
		** Get the GameSpy Query port number.
		*/
		int gsqport = cUserOptions::GameSpyQueryPort.Get();
		gsqport = ini.Get_Int(MasterServerSection, "GameSpyQueryPort", gsqport);
		if (!gsqport) gsqport = cUserOptions::GameSpyQueryPort.Get();
		if (gsqport < 0 || gsqport > 0xffff) {
			ConsoleBox.Print("Error - Invalid port number %d specified for GameSpy Query Port - aborting\n", gsqport);
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		}
		cUserOptions::GameSpyQueryPort.Set(gsqport);

		/*
		** Get the GameSpy Game port number.
		*/
		int gsgport = cUserOptions::GameSpyGamePort.Get();
		gsgport = ini.Get_Int(MasterServerSection, "GameSpyGamePort", gsgport);
		if (!gsgport) gsgport = cUserOptions::GameSpyGamePort.Get();
		if (gsgport < 0 || gsgport > 0xffff || gsgport == gsqport) {
			ConsoleBox.Print("Error - Invalid port number %d specified for GameSpy Game Port - aborting\n", gsgport);
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		}
		cUserOptions::GameSpyGamePort.Set(gsgport);

		/*
		** Get the bandwidth allowance.
		*/
		master_bw = ini.Get_Int(MasterServerSection, "BandwidthUp", 0xffffffff);
		if (master_bw != 0 && master_bw != 0xffffffff && master_bw < 33600) {
			ConsoleBox.Print("Error - Insufficient bandwidth specified for master server - aborting\n");
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		}
		MasterBandwidth = master_bw;

		/*
		** Get the max size of the disk log in days.
		*/
		DiskLogSize = ini.Get_Int(MasterServerSection, "DiskLogSize", 7);
		if (DiskLogSize > 365) {
			ConsoleBox.Print("Error - Disk log size too large - aborting\n");
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		}

		/*
		** Get the Network Update Rate override.
		*/
		int nur = ini.Get_Int(MasterServerSection, "NetUpdateRate", 8);
		if (nur < 5 || nur > 30) {
			ConsoleBox.Print("Error - NetUpdateRate must be between 5 and 30 - aborting\n");
			ConsoleBox.Wait_For_Keypress();;
			return(false);
		}
		cUserOptions::NetUpdateRate.Set(nur);

		/*
		** Get the remote admin settings.
		*/
		bool allow_remote = ini.Get_Bool(MasterServerSection, "AllowRemoteAdmin", false);
		RegistryClass reg_remote(APPLICATION_SUB_KEY_NAME_NET_SERVER_CONTROL);
		if (allow_remote) {
			ini.Get_String(MasterServerSection, "RemoteAdminPassword", "", remote_admin_pass, sizeof(remote_admin_pass));
			int len = strlen(remote_admin_pass);
			if (len == 0) {
				ConsoleBox.Print("Error - Remote admin password must be specified - aborting\n");
				ConsoleBox.Wait_For_Keypress();;
				return(false);
			} else {
				if (len > 31) {
					ConsoleBox.Print("Error - Remote admin password too long - aborting\n");
					ConsoleBox.Wait_For_Keypress();;
					return(false);
				}
			}
			int admin_port = ini.Get_Int(MasterServerSection, "RemoteAdminPort", 0);
			if (admin_port != 0 && (admin_port < 1024 || admin_port > 65535-8)) {
				ConsoleBox.Print("Error - Remote admin port number out of range - aborting\n");
				ConsoleBox.Wait_For_Keypress();;
				return(false);
			}

			ServerControl.Allow_Remote_Admin(true);

			/*
			** Set the port number into the registry.
			*/
			if (admin_port == 0) {
				admin_port = DEFAULT_SERVER_CONTROL_PORT;
			}
			reg_remote.Set_Int(SERVER_CONTROL_PORT_KEY, admin_port);

			/*
			** Set the password into the registry.
			*/
			reg_remote.Set_String(SERVER_CONTROL_PASSWORD_KEY, remote_admin_pass);

			/*
			** We need to bind to more than just the loopback address when listening for control messages.
			*/
			reg_remote.Set_Int(SERVER_CONTROL_LOOPBACK_KEY, 0);

			/*
			** There may be an IP override specified.
			*/
			ini.Get_String(MasterServerSection, "RemoteAdminIP", "0.0.0.0", remote_admin_ip, sizeof(remote_admin_ip));
			unsigned long admin_ip_long = ntohl(inet_addr(remote_admin_ip));
			reg_remote.Set_Int(SERVER_CONTROL_IP_KEY, admin_ip_long);

		} else {

			/*
			** Only listen to control messages on the loopback address.
			*/
			reg_remote.Set_Int(SERVER_CONTROL_LOOPBACK_KEY, 1);
			ServerControl.Allow_Remote_Admin(false);
		}

		/*
		** Set the master settings into the registry.
		*/
		//if (apply) {

			/*
			** Bandwidth.
			*/
			if (master_bw != 0xffffffff) {
				RegistryClass reg_netopt(APPLICATION_SUB_KEY_NAME_NETOPTIONS);
				if (reg_netopt.Is_Valid()) {
					cUserOptions::Set_Bandwidth_Type(BANDWIDTH_LANT1);
				}
			}
		//}
	}
	return(true);
}

/***********************************************************************************************
 * ServerSettingsClass::Encrypt_Serial -- Serial number encryption/decryption                  *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Input serial number                                                               *
 *           Reference to output serial number                                                 *
 *           Encrypt flag - true to encrypt, false to decrypt                                  *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/1/2002 12:08PM ST : Created                                                             *
 *=============================================================================================*/
void ServerSettingsClass::Encrypt_Serial(StringClass serial_in, StringClass &serial_out, bool encrypt)
{
	char *s;
	int numberlength = serial_in.Get_Length();
	unsigned long bytesread;
	char stringbuffer[ENCRYPTION_STRING_LENGTH];
	int p;

	s = new char [numberlength + 1];
	memcpy(s, serial_in.Peek_Buffer(), numberlength + 1);

	/*
	** See if the key file is available. If not, don't bother encrypting.
	*/
	HANDLE handle = CreateFile ("woldata.key", GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
	if (handle == INVALID_HANDLE_VALUE) {
		delete [] s;
		serial_out = serial_in;
		return;
	}

	/*
	** Read the key.
	*/
	if (!ReadFile(handle, stringbuffer, sizeof (stringbuffer), &bytesread, NULL)) {
		delete [] s;
		serial_out = serial_in;
		return;
	}

	int sign = encrypt ? 1 : -1;

	p = 0;
	for (unsigned i = 0; i < ENCRYPTION_STRING_LENGTH; i++) {

		int  t;
		char c;

		t  = s[p] - '0';
		t %= 10;
		t += (sign * stringbuffer[i]);
		t += 1000;
		t %= 10;
		c  = t + '0';
		s[p] = c;
		p++;
		if (p == numberlength) {
			p = 0;
		}
	}

	serial_out = StringClass(s, true);

	delete [] s;
}

/***********************************************************************************************
 * ServerSettingsClass::Decrypt_Serial -- Serial number decryption                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Input serial number                                                               *
 *           Reference to output serial number                                                 *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/1/2002 12:08PM ST : Created                                                             *
 *=============================================================================================*/
void ServerSettingsClass::Decrypt_Serial(StringClass serial_in, StringClass &serial_out)
{
	Encrypt_Serial(serial_in, serial_out, false);
}

/***********************************************************************************************
 * ServerSettingsClass::Check_Game_Settings_File -- Check game settings for validity           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Game settings .ini file name                                                      *
 *                                                                                             *
 * OUTPUT:   True if valid.                                                                    *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   2/3/2002 9:37PM ST : Created                                                              *
 *=============================================================================================*/
bool ServerSettingsClass::Check_Game_Settings_File(char *config_file)
{
	cGameDataCnc *game_settings = (cGameDataCnc*) cGameData::Create_Game_Of_Type(cGameData::GAME_TYPE_CNC);

	if (game_settings) {
		game_settings->Set_Ini_Filename(config_file);
		game_settings->Load_From_Server_Config();

		WideStringClass outMsg;
		bool ok = game_settings->Is_Valid_Settings(outMsg, true);

		delete game_settings;
		return(ok);
	}
	return(false);
}
