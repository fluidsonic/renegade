
#ifndef LCW_H
#define LCW_H

#ifdef _UNIX
#include "osdep.h"
#endif

int LCW_Uncomp(void const * source, void * dest, unsigned long length=0);

extern "C" {
int __cdecl LCW_Comp(void const * source, void * dest, int length);
}

#endif
