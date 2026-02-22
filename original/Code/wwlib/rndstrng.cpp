#include "global.h"
#include	"rndstrng.h"
#include	"wwstring.h"

/*
**
*/

void RandomStringClass::Add_String( const char * str )
{
	Strings.Add( str );
}

const char * RandomStringClass::Get_String( void )
{
	if ( Strings.Count() == 0 ) {
		return NULL;
	}
	int index = static_cast<int32_t>(static_cast<uint32_t>(Randomizer()) % static_cast<uint32_t>(Strings.Count()));
	return Strings[index];
}
