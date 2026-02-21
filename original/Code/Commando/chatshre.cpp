//
// Filename:     chatshre.cpp
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#include "chatshre.h"

#include "wwdebug.h"

//-----------------------------------------------------------------------------

#define ADD_CASE(exp) case exp: return #exp; break;  
LPCSTR Translate_Location(ChatLocationEnum location)
{
   switch (location) {
		
		ADD_CASE(WOLLOC_EXIT);
		ADD_CASE(WOLLOC_NOLIST);
		ADD_CASE(WOLLOC_LOBBYLIST);
		ADD_CASE(WOLLOC_LOBBY);
		ADD_CASE(WOLLOC_GAMESLIST);
		ADD_CASE(WOLLOC_GAMEDATA);
		ADD_CASE(WOLLOC_INGAME);

		ADD_CASE(LANLOC_EXIT);
		ADD_CASE(LANLOC_LOBBY);
		ADD_CASE(LANLOC_GAMESLIST);
		ADD_CASE(LANLOC_GAMEDATA);
		ADD_CASE(LANLOC_INGAME);

		default: 
			WWASSERT(0);
			return ""; // to avoid compiler warning
	}
}
