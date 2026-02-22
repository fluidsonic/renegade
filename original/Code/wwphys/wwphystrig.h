#pragma once

#include "global.h"

/*
** Debug triggers for WWPhys
** These are ID's for various debugging operations that the application can
** trigger inside of WWPhys.  Just hook a trigger handler into the WWDebug library
** and when you see the trigger id come in, return true or false depending on whether
** the you want to trigger the behavior
*/
enum
{
	WWPHYS_TRIGGER_COLLISION_DEBUGGING =			0x200,
	WWPHYS_TRIGGER_COLLISION_DISPLAY =				0x201,
	WWPHYS_TRIGGER_INVERT_VIS =						0x202,
	WWPHYS_TRIGGER_DISABLE_VIS =						0x203,
};
