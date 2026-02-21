//
// Filename:     playertype.cpp
// Author:       Byon Garrabrant
// Date:         Jun 2001
// Description:  
//
//-----------------------------------------------------------------------------

#include "playertype.h"

const char * Player_Type_Name( int player_type )
{
	switch( player_type ) {
		case 	PLAYERTYPE_SPECTATOR:	return "Spectator";
   	case 	PLAYERTYPE_MUTANT:		return "Mutant";
		case 	PLAYERTYPE_NEUTRAL:		return "Neutral";
		case 	PLAYERTYPE_RENEGADE:		return "Renegade";
		case 	PLAYERTYPE_NOD:			return "NOD";
		case 	PLAYERTYPE_GDI:			return "GDI";
	}
	return "Unknown";
}

