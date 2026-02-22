#include "global.h"
#include "networkobjectfactory.h"
#include "networkobjectfactorymgr.h"

/////////////////////////////////////////////////////////
//
//	NetworkObjectFactoryClass
//
/////////////////////////////////////////////////////////
NetworkObjectFactoryClass::NetworkObjectFactoryClass (void)	:
	NextFactory (0),
	PrevFactory (0)
{
	NetworkObjectFactoryMgrClass::Register_Factory (this);
	return ;
}

/////////////////////////////////////////////////////////
//
//	~NetworkObjectFactoryClass
//
/////////////////////////////////////////////////////////
NetworkObjectFactoryClass::~NetworkObjectFactoryClass (void)
{
	NetworkObjectFactoryMgrClass::Unregister_Factory (this);
	return ;
}
