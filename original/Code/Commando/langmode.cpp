#include "langmode.h" // I WANNA BE FIRST!
#include "miscutil.h"
#include "wwdebug.h"
#include "wwprofile.h"
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
	WWDEBUG_SAY(("LanGameModeClass::Init\n"));

	WWASSERT(PLanChat == NULL);
	PLanChat = new cLanChat();
	WWASSERT(PLanChat != NULL);
}

//-----------------------------------------------------------------------------
//
// called when the mode is deactivated
//
void LanGameModeClass::Shutdown(void)
{
	WWDEBUG_SAY(("LanGameModeClass::Shutdown"));

	WWASSERT(PLanChat != NULL);
	delete PLanChat;
	PLanChat = NULL;
}

//-----------------------------------------------------------------------------
//
// called each time through the main loop
//
void LanGameModeClass::Think(void)
{
	WWPROFILE( "Lan Think" );

	WWASSERT(PLanChat != NULL);
	PLanChat->Think();
}

//-----------------------------------------------------------------------------
cLanChat * LanGameModeClass::Get_Lan_Interface(void)
{
	//WWASSERT(PLanChat != NULL);

	return PLanChat;
}
