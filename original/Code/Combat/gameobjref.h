#pragma once

#include "global.h"


#include "reflist.h"

/*
**
*/
class		ScriptableGameObj;
typedef	ScriptableGameObj	*									GameObjRefPtr;
typedef	ReferenceableClass<ScriptableGameObj>			ReferenceableGameObj;
typedef	ReferencerClass										GameObjReference;
typedef	RefCountedReferencerClass							RefCountedGameObjReference;
