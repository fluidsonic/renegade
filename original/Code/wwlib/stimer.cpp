#include	"always.h"
#include	"stimer.h"
#include	"win.h"

#include "systimer.h"

long SystemTimerClass::operator () (void) const
{
	return TIMEGETTIME()/16;
}

SystemTimerClass::operator long (void) const
{
	return TIMEGETTIME()/16;
}