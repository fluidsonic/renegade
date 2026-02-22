#pragma once

#include "global.h"

#include "osdep.h"

int LCW_Uncomp(void const * source, void * dest, unsigned long length=0);

extern "C" {
int __cdecl LCW_Comp(void const * source, void * dest, int length);
}
