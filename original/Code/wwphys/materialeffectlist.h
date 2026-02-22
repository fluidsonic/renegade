#pragma once

#include "global.h"

#include "multilist.h"

class MaterialEffectClass;

typedef MultiListClass<MaterialEffectClass>			NonRefMaterialEffectListClass;
typedef MultiListIterator<MaterialEffectClass>		NonRefMaterialEffectListIterator;

typedef RefMultiListClass<MaterialEffectClass>		RefMaterialEffectListClass;
typedef RefMultiListIterator<MaterialEffectClass>	RefMaterialEffectListIterator;
