#pragma once

#include "global.h"


class PhysicalGameObj;

/*
**
*/
class ObjectLibraryManager {

public:
	// Create an object type from the library
	static PhysicalGameObj	*Create_Object( int type );
	static PhysicalGameObj	*Create_Object( const char *name );
};
