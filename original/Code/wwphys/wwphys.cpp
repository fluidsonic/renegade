#include "wwphys.h"
#include "physcon.h"
#include "physresourcemgr.h"


void WWPhys::Init(void)
{
	PhysicsConstants::Init();
	PhysResourceMgrClass::Init();
}

void WWPhys::Shutdown(void)
{
	PhysResourceMgrClass::Shutdown();
}


