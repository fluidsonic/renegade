#include "hash.h"
#include "realcrc.h"
#ifdef _UNIX
#include "osdep.h"
#endif

#include <string.h>

/*
** HashTableClass
*/
HashTableClass::HashTableClass( int size ) :
	HashTableSize( size )
{
	// Assert HashTableSize is a power of 2

	// Allocate and clear the table
	HashTable = new HashableClass * [ HashTableSize ];
	Reset();
}

HashTableClass::~HashTableClass( void )
{
	// If we need to, free the hash table
	if ( HashTable != NULL) {
		delete [] HashTable;
		HashTable = NULL;
	}
}

void	HashTableClass::Reset( void )
{
	for ( int i = 0; i < HashTableSize; i++ ) {
		HashTable[i] = NULL;
	}
}

void	HashTableClass::Add( HashableClass * entry )
{

	int index = Hash( entry->Get_Key() );
	entry->NextHash = HashTable[ index ];
	HashTable[ index ] = entry;
}

bool	HashTableClass::Remove( HashableClass * entry )
{

	// Find in the hash table.
	const char *key = entry->Get_Key();
	int index = Hash( key );

	if ( HashTable[ index ] != NULL ) {

		// Special check for first entry
		if ( HashTable[ index ] == entry ) {
			HashTable[ index ] = entry->NextHash;
			return true;
		}

		// Search the list for the entry, and remove it
		HashableClass * node = HashTable[ index ];
		while ( node->NextHash != NULL ) {
			if ( node->NextHash == entry ) {
				node->NextHash = entry->NextHash;
				return true;
			}
			node = node->NextHash;
		}
	}

	return false;
}

HashableClass * HashTableClass::Find( const char * key )
{
	// Find in the hash table.
	int index = Hash( key );
	for ( HashableClass * node = HashTable[ index ]; node != NULL; node = node->NextHash ) {
		if ( ::stricmp( node->Get_Key(), key ) == 0 ) {
			return node;
		}
	}
	return NULL;
}

int	HashTableClass::Hash( const char * key )
{
	return CRC_Stringi( key ) & (HashTableSize-1);
}


/*
**
*/
void	HashTableIteratorClass::First(void)
{
	Index = 0;
	NextEntry = Table.HashTable[ Index ];
	Advance_Next();
	Next();		// Accept the next we found, and go to the next next
}

void	HashTableIteratorClass::Next(void)
{
	CurrentEntry = NextEntry;
	if ( NextEntry != NULL ) {
		NextEntry = NextEntry->NextHash;
		Advance_Next();
	}
}

void	HashTableIteratorClass::Advance_Next(void)
{
	while ( NextEntry == NULL ) {
		Index++;
		if ( Index >= Table.HashTableSize ) {
			return;	// Done!
		}
		NextEntry = Table.HashTable[ Index ];
	}
}


