#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

#ifndef LCW_H
#define LCW_H

#ifdef _UNIX
#include "osdep.h"
#endif

int LCW_Uncomp(void const * source, void * dest, unsigned long length=0);

#ifdef _MSC_VER
int LCW_Comp(void const * source, void * dest, int length);
#else
extern "C" {
int __cdecl LCW_Comp(void const * source, void * dest, int length);
}
#endif

#endif
