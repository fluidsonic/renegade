#ifndef	GAMEOBJREF_H
#define	GAMEOBJREF_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

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

#endif	//	GAMEOBJREF_H
