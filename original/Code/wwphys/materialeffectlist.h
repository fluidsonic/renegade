#if defined(_MSC_VER)
#pragma once
#endif

#ifndef MATERIALEFFECTLIST_H
#define MATERIALEFFECTLIST_H

#include "always.h"
#include "multilist.h"

class MaterialEffectClass;

typedef MultiListClass<MaterialEffectClass>			NonRefMaterialEffectListClass;
typedef MultiListIterator<MaterialEffectClass>		NonRefMaterialEffectListIterator;

typedef RefMultiListClass<MaterialEffectClass>		RefMaterialEffectListClass;
typedef RefMultiListIterator<MaterialEffectClass>	RefMaterialEffectListIterator;


#endif //MATERIALEFFECTLIST_H



