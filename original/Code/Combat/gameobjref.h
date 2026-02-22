#pragma once

#include "global.h"


#ifndef	REFLIST_H
	#include "reflist.h"
#endif

/*
**
*/
class		ScriptableGameObj;
typedef	ScriptableGameObj	*									GameObjRefPtr;
typedef	ReferenceableClass<ScriptableGameObj>			ReferenceableGameObj;
typedef	ReferencerClass										GameObjReference;
typedef	RefCountedReferencerClass							RefCountedGameObjReference;
