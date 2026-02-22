#pragma once

#include "global.h"

//
// Filename:     devoptions.h
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:
//
//-----------------------------------------------------------------------------


#include "regbool.h"
#include "regint.h"
#include "boolean.h"

//-----------------------------------------------------------------------------
//
// Various options used for developing and testing
//
class cDevOptions
{
	public:

   //
   // QuickFullExit is a quick but hopefully clean way to leave combat (via
   // a keypress), and stop execution.
   // If running as a server, clients will be instructed to exit also (debug only).
   //
   static cBoolean QuickFullExit;

	static cRegistryBool ExitThreadOnAssert;
	static cRegistryBool CompareExeVersionOnNetwork;
   static cRegistryBool ShowFps;

	// TEMP. ST - 12/10/2001 3:39PM
	static cRegistryBool UseNewTCADO;

   private:

};

//-----------------------------------------------------------------------------

	//static cRegistryBool DoThumbnailPreInit;
