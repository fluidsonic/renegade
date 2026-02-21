#include	"always.h"
#include	"win.h"
#include	"mpu.h"
#include "math.h"
#include <assert.h>

typedef union {
	LARGE_INTEGER LargeInt;
	struct QuadPart {
		unsigned long LowPart;
		unsigned long HighPart;
	} QuadPart;
} QuadValue;


/***********************************************************************************************
 * Get_CPU_Rate -- Fetch the rate of CPU ticks per second.                                     *
 *                                                                                             *
 *    This routine will query the CPU to determine how many clock per second it is.            *
 *                                                                                             *
 * INPUT:   high  -- Reference to the location that will be filled with the upper 32 bits      *
 *                   of the result.                                                            *
 *                                                                                             *
 * OUTPUT:  Returns with the lower 32 bits of the result.                                      *
 *                                                                                             *
 * WARNINGS:   none                                                                            *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   05/20/1997 JLB : Created.                                                                 *
 *=============================================================================================*/
unsigned long Get_CPU_Rate(unsigned long & high)
{
	union {
		LARGE_INTEGER LargeInt;
		struct {
			unsigned long LowPart;
			unsigned long HighPart;
		} QuadPart;
	} value;

	if (QueryPerformanceFrequency(&value.LargeInt)) {
		high = value.QuadPart.HighPart;
		return(value.QuadPart.LowPart);
	}
	high = 0;
	return(0);
}


unsigned long Get_CPU_Clock(unsigned long & high)
{
	high = 0;
	return 0;
}




/*
**
** Cut and paste job from an intel example.
**
**
**
**
**
**
*/

#define ASM_RDTSC

// Max # of samplings to allow before giving up and returning current average.
#define MAX_TRIES			20
#define ROUND_THRESHOLD		6

// # of MHz to allow samplings to deviate from average of samplings.
#define TOLERANCE			1

static unsigned long TSC_Low;
static unsigned long TSC_High;

void RDTSC(void) { TSC_Low = 0; TSC_High = 0; }


int Get_RDTSC_CPU_Speed(void) { return 0; }


