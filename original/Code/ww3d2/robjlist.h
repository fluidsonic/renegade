#pragma once

#include "global.h"


#include "multilist.h"


class RenderObjClass;

typedef RefMultiListClass<RenderObjClass>		RefRenderObjListClass;
typedef RefMultiListIterator<RenderObjClass>	RefRenderObjListIterator;

typedef MultiListClass<RenderObjClass>			NonRefRenderObjListClass;
typedef MultiListIterator<RenderObjClass>		NonRefRenderObjListIterator;
