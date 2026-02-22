#pragma once

#include "global.h"

#include "widestring.h"

//-----------------------------------------------------------------------------
// Minimal cGameSpyAdmin — static storage and Think() hook for +connect CLI arg handling.
// No GameSpy auth, QnR, bandwidth detection, WOL dialogs, or nickname collision.
//-----------------------------------------------------------------------------
class cGameSpyAdmin
{
public:
	static void Think(void);
	static void Reset(void);
	static bool Is_Gamespy_Game(void);

	static void     Set_Game_Host_Ip(uint32_t ip)                         { GameHostIp = ip; }
	static uint32_t Get_Game_Host_Ip(void)                                { return GameHostIp; }
	static void     Set_Game_Host_Port(uint16_t port)                     { GameHostPort = port; }
	static uint16_t Get_Game_Host_Port(void)                              { return GameHostPort; }
	static void     Set_Is_Launch_From_Gamespy_Requested(bool flag)       { IsLaunchFromGamespyRequested = flag; }
	static bool     Get_Is_Launch_From_Gamespy_Requested(void)            { return IsLaunchFromGamespyRequested; }
	static void     Set_Is_Launched_From_Gamespy(bool flag)               { IsLaunchedFromGamespy = flag; }
	static bool     Get_Is_Launched_From_Gamespy(void)                    { return IsLaunchedFromGamespy; }
	static void     Set_Password_Attempt(WideStringClass & password)      { PasswordAttempt = password; }
	static WideStringClass & Get_Password_Attempt(void)                   { return PasswordAttempt; }

private:
	static void Connect_To_Game_Server(void);

	static bool           IsLaunchFromGamespyRequested;
	static bool           IsLaunchedFromGamespy;
	static uint32_t       GameHostIp;
	static uint16_t       GameHostPort;
	static WideStringClass PasswordAttempt;
};
