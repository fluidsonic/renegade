//
// Filename:     chatshre.h
// Project:      Network.lib, for Commando
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef CHATSHRE_H
#define CHATSHRE_H

#include "bittype.h"

//-----------------------------------------------------------------------------
enum ChatLocationEnum {
   
   //
	// Wol interface
	//
   WOLLOC_EXIT,
   WOLLOC_NOLIST,
   WOLLOC_LOBBYLIST,
   WOLLOC_LOBBY,
   WOLLOC_GAMESLIST,
	WOLLOC_GAMEDATA,
   WOLLOC_INGAME,

   //
	// Lan interface
	//
	LANLOC_EXIT,
	LANLOC_LOBBY,
	LANLOC_GAMESLIST,
	LANLOC_GAMEDATA,
   LANLOC_INGAME,
};

LPCSTR Translate_Location(ChatLocationEnum location);

//-----------------------------------------------------------------------------

#endif // CHATSHRE_H
  