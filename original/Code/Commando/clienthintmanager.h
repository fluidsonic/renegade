#pragma once

#include "global.h"

#include "vector.h"
class NetworkObjectClass;

//-----------------------------------------------------------------------------
class	cClientHintManager
{
public:
	static void		Think(void);

private:
	static int __cdecl Priority_Compare(const void **object1, const void **object2);

};

//-----------------------------------------------------------------------------
