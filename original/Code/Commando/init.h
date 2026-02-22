#ifndef INIT_H
#define INIT_H

#ifndef ALWAYS_H
#include "always.h"
#endif

#include "ffactory.h"

void	Get_Version_Number(DWORD *major, DWORD *minor);
bool	Game_Init(void);
void	Install_Fonts (bool onoff);
void	Application_Exception_Callback(void);

#define VALUE_NAME_DISABLE_SERVER_SYSINFO_COLLECTING "DisableServerSysInfoCollecting"
#define VALUE_NAME_GAME_INITIALIZATION_IN_PROGRESS "GameInitInProgress"
#define VALUE_NAME_APPLICATION_CRASH_VERSION "ApplicationCrashVersion"
#define VALUE_NAME_DISABLE_LOG_COPYING "DisableLogCopying"

extern SimpleFileFactoryClass	RenegadeBaseFileFactory;

#endif
