
#ifndef DX8LIST_H
#define DX8LIST_H

#include "always.h"
#include "multilist.h"

/*
** Here we're just typedefing some multi-lists so we don't have to write the
** long template names.
*/
class DX8TextureCategoryClass;
typedef MultiListClass<DX8TextureCategoryClass>			TextureCategoryList;
typedef MultiListIterator<DX8TextureCategoryClass>		TextureCategoryListIterator;

class DX8FVFCategoryContainer;
typedef MultiListClass<DX8FVFCategoryContainer>			FVFCategoryList;
typedef MultiListIterator<DX8FVFCategoryContainer>		FVFCategoryListIterator;

class DX8PolygonRendererClass;
typedef MultiListClass<DX8PolygonRendererClass>			DX8PolygonRendererList;
typedef MultiListIterator<DX8PolygonRendererClass>		DX8PolygonRendererListIterator; 

class DX8TextureTrackerClass;
typedef MultiListClass<DX8TextureTrackerClass>			DX8TextureTrackerList;
typedef MultiListIterator<DX8TextureTrackerClass>		DX8TextureTrackerListIterator;

#endif //DX8LIST_H

