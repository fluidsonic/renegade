//
// Filename:     playertype.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1998
// Description:  
//
//-----------------------------------------------------------------------------

#ifndef PLAYERTYPE_H
#define PLAYERTYPE_H

// 4514: unreferenced inline function has been removed.

//-----------------------------------------------------------------------------
typedef enum {

	PLAYERTYPE_FIRST					= -4,

	PLAYERTYPE_SPECTATOR				= PLAYERTYPE_FIRST,	// -4
	PLAYERTYPE_MUTANT,											// -3
	PLAYERTYPE_NEUTRAL,											// -2
	PLAYERTYPE_RENEGADE,											// -1
	PLAYERTYPE_NOD,												//  0
	PLAYERTYPE_GDI,												//  1

	PLAYERTYPE_LAST					= PLAYERTYPE_GDI,

	PLAYERTYPE_MUTATION_MUTATED	= PLAYERTYPE_NOD,
	PLAYERTYPE_MUTATION_REGULAR	= PLAYERTYPE_GDI,
} PLAYERTYPE;

	//PLAYERTYPE_LAST_TEAM = PLAYERTYPE_GDI,
	//PLAYERTYPE_CIVILIAN = PLAYERTYPE_NEUTRAL,
	//PLAYERTYPE_FIRST_TEAM = PLAYERTYPE_NOD,

const char * Player_Type_Name( int player_type );

//-----------------------------------------------------------------------------

inline bool	Player_Types_Are_Enemies( int player_type_1, int player_type_2 )
{
	// if either are a spectator or neutral, they are not enemies
	if (( player_type_1 == PLAYERTYPE_NEUTRAL ) ||
		 ( player_type_2 == PLAYERTYPE_NEUTRAL ) ) {
		return false;
	}

	if (( player_type_1 == PLAYERTYPE_SPECTATOR ) ||
		 ( player_type_2 == PLAYERTYPE_SPECTATOR ) ) {
		return false;
	}

	// if either is Renegade, they are enemies
	if (( player_type_1 == PLAYERTYPE_RENEGADE ) ||
		 ( player_type_2 == PLAYERTYPE_RENEGADE ) ) {
		return true;
	}

	// iff they are not the same type, they are enemies
	return ( player_type_1 != player_type_2 );
}

#endif // PLAYERTYPE_H
