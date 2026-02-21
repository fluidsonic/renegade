#pragma once

#ifndef _SLAVEMASTER_H
#define _SLAVEMASTER_H

#include <winbase.h>

#define MAX_SLAVES	7

class SlaveMasterClass;

/*
**
**  One of these classes for each slave server.
**
*/
class SlaveServerClass
{
	friend SlaveMasterClass;

	public:

		SlaveServerClass(void);
		~SlaveServerClass(void);

		void Set(bool enable, char *nick, char *serial, unsigned short port, char *settings_file, int bandwidth, char *password);
		void Get(bool &enable, char *nick, char *serial, unsigned short &port, char *settings_file, int &bandwidth, char *password);


	private:

		char	NickName[32];
		char	Serial[64];
		char	Password[64];
		unsigned short Port;
		char	SettingsFileName[MAX_PATH];

		bool	Enable;
		bool	IsRunning;
		unsigned short ControlPort;
		int	Bandwidth;

		PROCESS_INFORMATION ProcessInfo;
};







/*
**
**  Master server uses this class to manage slave servers.
**
**
*/
class SlaveMasterClass
{
	public:

		SlaveMasterClass(void);
		~SlaveMasterClass(void);

		void Startup_Slaves(void);
		void Shutdown_Slaves(void);
		bool Shutdown_Slave(char *slave_login);

		char *Get_Slave_Info(char *buffer, int buflen);

		void Load(void);
		void Save(void);
		void Reset(void);

		int Get_Num_Slaves(void) {return(NumSlaveServers);}
		int Get_Num_Enabled_Slaves(void);
		void Add_Slave(bool enable, char *nick, char *serial, unsigned short port, char *settings_file, int bandwidth, char *password);
		SlaveServerClass *Get_Slave(int index);

		void Set_Slave_Mode(bool mode) {SlaveMode = mode;}
		bool Am_I_Slave(void) {return(SlaveMode);}


	private:

		void Delete_Registry_Copies(void);
		void Create_Registry_Copies(void);
		bool Aquire_Slave(int index);
		void Wait_For_Slave_Shutdown(void);

		SlaveServerClass SlaveServers[MAX_SLAVES];
		int NumSlaveServers;

		bool SlaveMode;	// false = master, true = slave

};



extern SlaveMasterClass SlaveMaster;













#endif //_SLAVEMASTER_H