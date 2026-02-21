#ifndef _MISSION10_H_
#define _MISSION10_H_


// Defines and Includes

#include "toolkit.h"


// Predefined Constants

// Public Variables

// Enumerations
#define MAMMOTH							40000
#define ATTACK							40001
#define KILLED							40002
#define TARGET							40003
#define HON								40004
#define GRANT							40005
#define	HELIPAD							40006
#define GOTO_LOC						40007
#define FAKE_TIMER						40008
#define HAVOCS_SCRIPT					40009
#define MISSION_COMPLETE				40010
#define REBUILD							40011
#define M10_PLAYERTYPE_CHANGE_OBELISK	40012
#define FLYOVER							40013
#define	REMOVE_SECONDARY_POG			40014
#define ENTERED							40015
#define CLEAR1							40016
#define CLEAR2							40017
#define GRANT_MRLS						40018
#define OCCUPIED						40019
#define TANK_KILLED						40020
#define CARGO_DROP						40021
#define REINFORCE						40022
#define NO_DROP							40023
#define KEY_OBJ							40024
#define DAMAGED							40025
#define RESET							40026
#define KANE_CONV						40027
#define MAMMY							40028

// Timer Enumerations

typedef enum
{
	M10TIMER_START = STIMER_MISSION10,
} M10TIMER;

#endif // _MISSION10_H_
