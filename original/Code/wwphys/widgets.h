#if defined(_MSC_VER)
#pragma once
#endif

#ifndef WIDGETS_H
#define WIDGETS_H

class RenderObjClass;

/*
** Debug Widgets.  These render objects will be used by the physics debugging code
** to render bounding boxes, force vectors, co-ordinate axes, etc.  These are meant
** to be rendered immediately and then released because someone else could come along
** and get a pointer to the same object.  
*/
class WidgetSystem
{
public:

	enum WidgetType
	{
		WIDGET_AABOX = 0,		// an AABoxRenderObjClass
		WIDGET_OBBOX,			// an OBBoxRenderObjClass
		WIDGET_VECTOR,			// a skin, designed to point along -z (use with lookat) with bones Point0, Point1
		WIDGET_AXES,			// a mesh with red,green,and blue axes for +x,+y,+z
		WIDGET_POINT,			// a 0.1m sphere
		NUM_WIDGETS
	};

	static void						Init_Debug_Widgets(void);
	static void						Release_Debug_Widgets(void);
	static RenderObjClass *		Get_Debug_Widget(WidgetType id);

protected:

	/*
	** Debug widgets.  These are render objects that are used to aid in debugging
	** the physics system.  They are allocated in Init_Debug_Widgets and released
	** in Release_Debug_Widgets.  Users can get at them through the Get_Debug_Widget
	** call.  They are meant to be rendered immediately and released.
	*/
#ifdef WWDEBUG
	static RenderObjClass *	DebugWidgets[NUM_WIDGETS];
#endif
};

#endif

