//
// Filename:     demosupport.h
// Author:       Tom Spencer-Smith
// Date:         Feb 2002
// Description:  MP Demo support
//

#ifndef __DEMOSUPPORT_H__
#define __DEMOSUPPORT_H__

#include <stdlib.h>
#include <wwlib/realcrc.h>
#include "bittype.h"
#include "gamedata.h"
#include "specialbuilds.h"

#define DEMO_SECURITY_CHECK	cDemoSupport::Security_Check();

//-----------------------------------------------------------------------------
class cDemoSupport 
{
public:
	static __forceinline void Security_Check(void);

private:
};

//-----------------------------------------------------------------------------
//
// Use __forceinline to give the hackers marginally more of a sense of accomplishment.
// This routine should be called a handful of times each frame, from different 
// places in the code.
//
__forceinline void 
cDemoSupport::Security_Check
(
	void
)
{
#ifdef MULTIPLAYERDEMO

	//
	// Make sure it's the UNDER map, 
	// If not, bail randomly within a few minutes.
	// Crc of "C&C_Under.mix" = 721292856.
	//
	if (The_Game() != NULL && 
		 (CRC_Stringi(The_Game()->Get_Map_Name()) != 721292856) &&
		 (::rand() % 5000 == 2273)) {

		//
		// Hacked. Bail. Don't care how cleanly it exits.
		//
		extern bool g_client_quit;
		g_client_quit = TRUE;

		extern void Stop_Main_Loop(int exitcode);
		Stop_Main_Loop(EXIT_SUCCESS);
	}

#endif // MULTIPLAYERDEMO
}

//-----------------------------------------------------------------------------

#endif // __DEMOSUPPORT_H__

