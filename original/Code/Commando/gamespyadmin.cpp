#include "global.h"
#include "gamespyadmin.h"

#include "widestring.h"
#include "gameinitmgr.h"
#include "gamedata.h"
#include "cnetwork.h"
#include "DlgMPConnect.h"
#include "dialogtests.h"

//
// Class statics
//
bool            cGameSpyAdmin::IsLaunchFromGamespyRequested = false;
bool            cGameSpyAdmin::IsLaunchedFromGamespy        = false;
uint32_t        cGameSpyAdmin::GameHostIp                   = 0;
uint16_t        cGameSpyAdmin::GameHostPort                 = 0;
WideStringClass cGameSpyAdmin::PasswordAttempt;

//-----------------------------------------------------------------------------
void cGameSpyAdmin::Think(void)
{
	assert(Is_Gamespy_Game());

	if (IsLaunchFromGamespyRequested && SplashIntroMenuDialogClass::Is_Complete()) {
		Connect_To_Game_Server();
		IsLaunchFromGamespyRequested = false;
		IsLaunchedFromGamespy = true;
	}
}

//-----------------------------------------------------------------------------
void cGameSpyAdmin::Connect_To_Game_Server(void)
{
	assert(GameHostIp > 0);
	assert(GameHostPort > 0);

	GameInitMgrClass::Initialize_LAN();

	assert(PTheGameData == NULL);
	PTheGameData = cGameData::Create_Game_Of_Type(cGameData::GAME_TYPE_CNC);
	assert(PTheGameData != NULL);
	PTheGameData->Set_Ip_Address(GameHostIp);
	PTheGameData->Set_Port(GameHostPort);

	cNetwork::Init_Client();

	DlgMPConnect::DoDialog(-1);
}

//-----------------------------------------------------------------------------
void cGameSpyAdmin::Reset(void)
{
	IsLaunchFromGamespyRequested = false;
	IsLaunchedFromGamespy        = false;
	GameHostIp                   = 0;
	GameHostPort                 = 0;
	PasswordAttempt              = WideStringClass();
}

//-----------------------------------------------------------------------------
bool cGameSpyAdmin::Is_Gamespy_Game(void)
{
	return IsLaunchFromGamespyRequested || IsLaunchedFromGamespy;
}
