#ifndef	OBJLIBRARY_H
#define	OBJLIBRARY_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

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

#endif  // OBJLIBRARY_H

