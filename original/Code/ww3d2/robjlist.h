
#ifndef ROBJLIST_H
#define ROBJLIST_H

#ifndef ALWAYS_H
#include "always.h"
#endif

#ifndef MULTILIST_H
#include "multilist.h"
#endif

#ifndef WWDEBUG_H
#endif

class RenderObjClass;

typedef RefMultiListClass<RenderObjClass>		RefRenderObjListClass;
typedef RefMultiListIterator<RenderObjClass>	RefRenderObjListIterator;

typedef MultiListClass<RenderObjClass>			NonRefRenderObjListClass;
typedef MultiListIterator<RenderObjClass>		NonRefRenderObjListIterator;

#endif


