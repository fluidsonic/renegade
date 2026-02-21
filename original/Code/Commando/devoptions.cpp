//
// Filename:     devoptions.cpp
// Author:       Tom Spencer-Smith
// Date:         Dec 1999
// Description:
//

#include "devoptions.h" // I WANNA BE FIRST!

#include "_globals.h"
#include "player.h"
#include "registry.h"

//
// Class statics
//

   cBoolean cDevOptions::QuickFullExit(false);

   cRegistryBool cDevOptions::ExitThreadOnAssert(				APPLICATION_SUB_KEY_NAME_DEBUG,	"ExitThreadOnAssert",				true);
   cRegistryBool cDevOptions::CompareExeVersionOnNetwork(	APPLICATION_SUB_KEY_NAME_DEBUG,	"CompareExeVersionOnNetwork",		true);

	cRegistryBool cDevOptions::UseNewTCADO(						APPLICATION_SUB_KEY_NAME_DEBUG,	"NewTCADO",								true);
   cRegistryBool cDevOptions::ShowFps(								APPLICATION_SUB_KEY_NAME_NETDEBUG, "ShowFps",							false);

	//cRegistryBool cDevOptions::DoThumbnailPreInit(				APPLICATION_SUB_KEY_NAME_DEBUG,	"DoThumbnailPreInit",				true);