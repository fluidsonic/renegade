#pragma once

#include "global.h"

#include "saveloadids.h"

//////////////////////////////////////////////////////////////////////////////////
//
//	These are the chunk IDs that serve as 'globally-unique' persist identifiers for
//	all persist objects inside this library.  These are used when building the
//	PersistFactoryClass's for PersistClass-derived objects.
//
//////////////////////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_TRANSLATE_DB		= CHUNKID_WWTRANSLATEDB_BEGIN,
	CHUNKID_TRANSLATE_OBJ,
	CHUNKID_TDBCATEGORY,
	CHUNKID_STRING_TWIDDLER,
};
