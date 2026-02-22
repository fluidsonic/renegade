#pragma once

#include "global.h"

#include "multilist.h"

class PhysClass;

typedef RefMultiListClass<PhysClass>		RefPhysListClass;
typedef RefMultiListIterator<PhysClass>	RefPhysListIterator;

typedef MultiListClass<PhysClass>			NonRefPhysListClass;
typedef MultiListIterator<PhysClass>		NonRefPhysListIterator;
