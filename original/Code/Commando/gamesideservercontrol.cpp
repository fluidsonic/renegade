#include "global.h"
#include "gamesideservercontrol.h"
#include "registry.h"
#include "_globals.h"
#include "consolefunction.h"
#include "gamedata.h"
#include "servercontrol.h"
#include "gamemode.h"
#include "buildnum.h"
#include "consolemode.h"


/*
** Static class data.
*/
bool GameSideServerControlClass::Listening = false;
StringClass GameSideServerControlClass::Response;

/***********************************************************************************************
 * GameSideServerControlClass::Init -- Start listening for server control messages             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:55PM ST : Created                                                            *
 *=============================================================================================*/
void GameSideServerControlClass::Init(void)
{}

/***********************************************************************************************
 * GameSideServerControlClass::Set_Welcome_Message -- Update the welcome message               *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:56PM ST : Created                                                            *
 *=============================================================================================*/
void GameSideServerControlClass::Set_Welcome_Message(void)
{
	char buffer[1024];
	buffer[0] = 0;
	char welcome[1024];

	/*
	** Basic welcome message includes build info.
	*/
	sprintf(welcome, "Welcome to Renegade %s\n", BuildInfoClass::Composite_Build_Info());

	ServerControl.Set_Welcome_Message(welcome);
}

/***********************************************************************************************
 * GameSideServerControlClass::Shutdown -- Stop listening to server control messages           *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Nothing                                                                           *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:57PM ST : Created                                                            *
 *=============================================================================================*/
void GameSideServerControlClass::Shutdown(void)
{
	ServerControl.Stop_Listening();
	Listening = false;
}

/***********************************************************************************************
 * GameSideServerControlClass::App_Request_Callback -- Server control request handler          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Request text                                                                      *
 *                                                                                             *
 * OUTPUT:   Response text                                                                     *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:58PM ST : Created                                                            *
 *=============================================================================================*/
const char *GameSideServerControlClass::App_Request_Callback(char *request)
{
	Response.Erase(0, Response.Get_Length());
	if (Listening) {
		if (GameModeManager::Find("Combat")->Is_Active()) {
			ConsoleFunctionManager::Parse_Input(request);
		}
	}
	Response += "\n";
	return(Response.Peek_Buffer());
}

/***********************************************************************************************
 * GameSideServerControlClass::Print -- Adds to the response packet for the current request    *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Response text                                                                     *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:58PM ST : Created                                                            *
 *=============================================================================================*/
void GameSideServerControlClass::Print(char *text, ...)
{
	if (Listening) {
		if (Response.Get_Length() > 32768) {
			Response.Erase(0, Response.Get_Length());
		}

		char buffer[8192];

		va_list va;

		buffer[sizeof(buffer)-1] = 0;
		va_start(va, text);
		_vsnprintf(&buffer[0], sizeof(buffer)-1, text, va);
		va_end(va);

		Response += buffer;
	}
}

/***********************************************************************************************
 * GameSideServerControlClass::Send_Message -- Send a request message to another server        *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:    Message                                                                           *
 *           IP                                                                                *
 *           Port                                                                              *
 *                                                                                             *
 * OUTPUT:   Nothing                                                                           *
 *                                                                                             *
 * WARNINGS: None                                                                              *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   11/16/2001 3:59PM ST : Created                                                            *
 *=============================================================================================*/
void GameSideServerControlClass::Send_Message(char *text, unsigned long ip, unsigned short port)
{
	if (Listening) {
		ServerControl.Send_Message(text, ip, port);
	}
}
