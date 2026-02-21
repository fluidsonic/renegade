#if defined(_MSC_VER)
#pragma once
#endif


#ifndef WIDGETUSER_H
#define WIDGETUSER_H


class Vector3;
class Matrix3D;
class AABoxClass;
class OBBoxClass;
class RenderInfoClass;
class WidgetRenderOpClass;


/**
** WidgetUserClass
** This class contains the functions needed to embed a list of debug widgets into
** a class.  Just call Render_Debug_Widgets inside of your class's render function.
** It is important that all of the runtime cost of using this system goes away in
** a release build.  When you derive a class from this class, if you are overriding
** the Add_xxx methods bracket your implementations and declarations with #ifdef WWDEBUG
**
** Notes:
** Most of my current uses for this class are for "transient" things so I reset the list each
** time I render.  In addition, I override the 'Add' functions and add filters which
** check if debugging for that particular system is turned on.  Use the macros below the
** class definition so that all of your calls automatically disappear in the release build.
*/
class WidgetUserClass
{

public:

	WidgetUserClass(void);
	~WidgetUserClass(void);	

	/*
	** Debug rendering of vectors, points, boxes, etc etc.  Each frame, these objects
	** will be rendered during PhysClass::Render and then deleted.
	*/
	
#ifdef WWDEBUG
	void				Reset_Debug_Widget_List(void);
	virtual void	Add_Debug_Point(const Vector3 & p,const Vector3 & color);
	virtual void	Add_Debug_Vector(const Vector3 & p,const Vector3 & v,const Vector3 & color);
	virtual void	Add_Debug_AABox(const AABoxClass & box,const Vector3 & color,float opacity = 0.25f);
	virtual void	Add_Debug_OBBox(const OBBoxClass & box,const Vector3 & color,float opacity = 0.25f);
	virtual void	Add_Debug_Axes(const Matrix3D & transform,const Vector3 & color);
	void				Render_Debug_Widgets(RenderInfoClass & rinfo);
#else
	void				Reset_Debug_Widget_List(void)																	{}
	void				Add_Debug_Point(const Vector3 & p,const Vector3 & color)								{}
	void				Add_Debug_Vector(const Vector3 & p,const Vector3 & v,const Vector3 & color)	{}
	void				Add_Debug_AABox(const AABoxClass & box,const Vector3 & color,float opacity = 0.25f)	{}
	void				Add_Debug_OBBox(const OBBoxClass & box,const Vector3 & color,float opacity = 0.25f)	{}
	void				Add_Debug_Axes(const Matrix3D & transform,const Vector3 & color)					{}
	void				Render_Debug_Widgets(RenderInfoClass & rinfo)											{}
#endif

protected:

#ifdef WWDEBUG
	void				Add_Debug_Widget(WidgetRenderOpClass * op);	
#else
	void				Add_Debug_Widget(WidgetRenderOpClass * op)												{}
#endif

	/*
	** Debug widget rendering list
	** This member, which only exists when WWDEBUG is enabled, handles the rendering
	** of any debug widgets that this object needs.
	*/
#ifdef WWDEBUG
	WidgetRenderOpClass *			WidgetRenderOpList;
#endif

};




#endif