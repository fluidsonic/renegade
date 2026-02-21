#include "widgets.h"
#include "bin_aabox.h"
#include "bin_axes.h"
#include "bin_obbox.h"
#include "bin_point.h"
#include "bin_vector.h"
#include "assetmgr.h"
#include "rendobj.h"
#include "ramfile.h"

#ifdef WWDEBUG
struct WidgetDefStruct
{
	int							Id;
	const unsigned char *	Data;
	unsigned int				Size;
	const char *				Name;
	RenderObjClass *			RObj;
};

static WidgetDefStruct _WidgetDefs[WidgetSystem::NUM_WIDGETS] = 
{
	{ WidgetSystem::WIDGET_AABOX,	_AABoxBinary,	sizeof(_AABoxBinary),	"AABox.Box",NULL },
	{ WidgetSystem::WIDGET_OBBOX,	_OBBoxBinary,	sizeof(_OBBoxBinary),	"OBBox.Box",NULL },
	{ WidgetSystem::WIDGET_VECTOR,_VectorBinary,	sizeof(_VectorBinary),	"Vector",	NULL },
	{ WidgetSystem::WIDGET_AXES,	_AxesBinary,	sizeof(_AxesBinary),		"Axes",		NULL },
	{ WidgetSystem::WIDGET_POINT,	_PointBinary,	sizeof(_PointBinary),	"Point",		NULL }
};
#endif


void WidgetSystem::Init_Debug_Widgets(void)
{
#ifdef WWDEBUG //don't create debug widgets in release builds...

	for (int i=0; i<NUM_WIDGETS; i++) {

		WWASSERT(_WidgetDefs[i].Id == i);

		if (_WidgetDefs[i].RObj == NULL) {
			RAMFileClass file((void*)_WidgetDefs[i].Data,_WidgetDefs[i].Size);	
			WW3DAssetManager::Get_Instance()->Load_3D_Assets(file);
			_WidgetDefs[i].RObj = WW3DAssetManager::Get_Instance()->Create_Render_Obj(_WidgetDefs[i].Name);
			SET_REF_OWNER( _WidgetDefs[i].RObj );
		}
	}
#endif
}

void WidgetSystem::Release_Debug_Widgets(void)
{
#ifdef WWDEBUG
	for (int i=0; i<NUM_WIDGETS; i++) {
		REF_PTR_RELEASE(_WidgetDefs[i].RObj);
	}
#endif
}

RenderObjClass * WidgetSystem::Get_Debug_Widget(WidgetType id)
{
#ifdef WWDEBUG
	if (_WidgetDefs[id].RObj != NULL) {
		_WidgetDefs[id].RObj->Add_Ref();
	}
	return _WidgetDefs[id].RObj;
#else
	return NULL;
#endif
	
}
