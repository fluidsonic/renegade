#include "scripts.h"
#include "toolkit.h"
#include <stdio.h>
#include <time.h>

#ifdef WWDEBUG
#define SCRIPT_DEBUG_MESSAGE( X )	if (debug_mode)	{ Commands->Debug_Message X ; }
#else
#define SCRIPT_DEBUG_MESSAGE( X )
#endif

