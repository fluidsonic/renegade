#include "langmode.h" // I WANNA BE FIRST!
#include "miscutil.h"
//
// class statics
//
cLanChat * LanGameModeClass::PLanChat = NULL;

//-----------------------------------------------------------------------------
//
// called when the mode is activated
//
void LanGameModeClass::Init(void)
{

	PLanChat = new cLanChat();
}

//-----------------------------------------------------------------------------
//
// called when the mode is deactivated
//
void LanGameModeClass::Shutdown(void)
{

	delete PLanChat;
	PLanChat = NULL;
}

//-----------------------------------------------------------------------------
//
// called each time through the main loop
//
void LanGameModeClass::Think(void)
{

	PLanChat->Think();
}

//-----------------------------------------------------------------------------
cLanChat * LanGameModeClass::Get_Lan_Interface(void)
{
	//assert(PLanChat != NULL);

	return PLanChat;
}
