#if defined(_MSC_VER)
#pragma once
#endif

#ifndef PHYSLIST_H
#define PHYSLIST_H

#include "always.h"
#include "multilist.h"

class PhysClass;

typedef RefMultiListClass<PhysClass>		RefPhysListClass;
typedef RefMultiListIterator<PhysClass>	RefPhysListIterator;

typedef MultiListClass<PhysClass>			NonRefPhysListClass;
typedef MultiListIterator<PhysClass>		NonRefPhysListIterator;

#endif // PHYSLIST_H
