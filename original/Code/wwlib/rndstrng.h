#pragma once

#include "global.h"

#include	"vector.h"
#include	"random.h"

class	StringClass;

/*
**
*/
class	RandomStringClass {

public:
	// Add a string to the class.  
	// (future version may have a weight parameter)
	void Add_String( const char * str );

	// Get a random string from the class
	const char * Get_String( void );

private:
	DynamicVectorClass<StringClass>	Strings;
	Random2Class							Randomizer;
};
