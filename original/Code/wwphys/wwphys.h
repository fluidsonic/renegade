#pragma once

#include "global.h"

class WWPhys
{
public:

	/*
	** Init and Shutdown should be called once by the app (at initialization and at shutdown)
	*/
	static void Init(void);
	static void Shutdown(void);
};
