#include "networkobjectfactorymgr.h"
#include "networkobjectfactory.h"
#include "wwdebug.h"
#include <string.h>


////////////////////////////////////////////////////////////////////////////
//	Static member initialization
////////////////////////////////////////////////////////////////////////////
NetworkObjectFactoryClass *NetworkObjectFactoryMgrClass::_FactoryListHead = 0;


////////////////////////////////////////////////////////////////////////////
//
//	Find_Factory
//
////////////////////////////////////////////////////////////////////////////
NetworkObjectFactoryClass *
NetworkObjectFactoryMgrClass::Find_Factory (uint32 class_id)
{
	NetworkObjectFactoryClass *factory = 0;

	//
	//	Loop through all the factories and see if we can
	// find the one who owns the corresponding class-id.
	//
	for (	NetworkObjectFactoryClass *curr_factory = _FactoryListHead;
			(factory == 0) && (curr_factory != 0);
			curr_factory = curr_factory->NextFactory) {

		//
		//	Is this the factory we were looking for?
		//
		if (curr_factory->Get_Class_ID () == class_id) {
			factory = curr_factory;
		}
	}

	return factory;
}


////////////////////////////////////////////////////////////////////////////
//
//	Get_First
//
////////////////////////////////////////////////////////////////////////////
NetworkObjectFactoryClass *
NetworkObjectFactoryMgrClass::Get_First (void)
{
	return _FactoryListHead;
}


////////////////////////////////////////////////////////////////////////////
//
//	Get_Next
//
////////////////////////////////////////////////////////////////////////////
NetworkObjectFactoryClass *
NetworkObjectFactoryMgrClass::Get_Next (NetworkObjectFactoryClass *curr_factory)
{
	NetworkObjectFactoryClass *factory = 0;

	//
	//	Simply return the next factory in the chain
	//
	if (curr_factory != NULL) {
		factory = curr_factory->NextFactory;
	}

	return factory;
}


////////////////////////////////////////////////////////////////////////////
//
//	Register_Factory
//
////////////////////////////////////////////////////////////////////////////
void
NetworkObjectFactoryMgrClass::Register_Factory (NetworkObjectFactoryClass *factory)
{
	WWASSERT (factory->NextFactory == 0);
	WWASSERT (factory->PrevFactory == 0);
	Link_Factory (factory);
	return ;
}


////////////////////////////////////////////////////////////////////////////
//
//	Unregister_Factory
//
////////////////////////////////////////////////////////////////////////////
void
NetworkObjectFactoryMgrClass::Unregister_Factory (NetworkObjectFactoryClass *factory)
{
	WWASSERT (factory != 0);
	Unlink_Factory (factory);
	return ;
}


////////////////////////////////////////////////////////////////////////////
//
//	Link_Factory
//
////////////////////////////////////////////////////////////////////////////
void
NetworkObjectFactoryMgrClass::Link_Factory (NetworkObjectFactoryClass *factory)
{
	WWASSERT (factory->NextFactory == 0);
	WWASSERT (factory->PrevFactory == 0);

	// Adding this factory in front of the current head of the list
	factory->NextFactory = _FactoryListHead;
	
	// If the list wasn't empty, link the next factory back to this factory
	if (factory->NextFactory != 0) {
		factory->NextFactory->PrevFactory = factory;
	}

	// Point the head of the list at this factory now
	_FactoryListHead = factory;
	return ;
}


////////////////////////////////////////////////////////////////////////////
//
//	Unlink_Factory
//
////////////////////////////////////////////////////////////////////////////
void
NetworkObjectFactoryMgrClass::Unlink_Factory (NetworkObjectFactoryClass *factory)
{
	WWASSERT(factory != 0);

	// Handle the factory's prev pointer:
	if (factory->PrevFactory == 0) {

		// this factory is the head
		WWASSERT (_FactoryListHead == factory);
		_FactoryListHead = factory->NextFactory;
	
	} else {

		// link it's prev with it's next
		factory->PrevFactory->NextFactory = factory->NextFactory;

	}

	// Handle the factory's next pointer if its not at the end of the list:
	if (factory->NextFactory != 0) {
		
		factory->NextFactory->PrevFactory = factory->PrevFactory;

	}

	// factory is now un-linked
	factory->NextFactory = 0;
	factory->PrevFactory = 0;
	return ;
}
