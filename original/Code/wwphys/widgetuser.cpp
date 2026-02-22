#include "global.h"
#include "widgetuser.h"
#include "widgets.h"
#include "mempool.h"
#include "vertmaterial.h"
#include "matinfo.h"
#include "boxrobj.h"
#include "ww3d.h"

/*
** WidgetRenderOpClass
** This class encapsulates the instructions from a physics object to render
** a debug widget.  Each physics object has a list of these hanging off it 
** for rendering vectors, points, etc.
** Since instances of this class are constantly allocated and deallocated
** each frame they are derived from AutoPoolClass.
*/
class WidgetRenderOpClass : public AutoPoolClass<WidgetRenderOpClass,256>
{
public:

	WidgetRenderOpClass(void);
	
	void	Set_Color(const Vector3 & color);
	void	Set_Opacity(float opacity);
	void	Init_Point(const Vector3 & point);
	void	Init_Vector(const Vector3 & point,const Vector3 & vector);
	void	Init_AABox(const AABoxClass & box);
	void	Init_OBBox(const OBBoxClass & box);
	void	Init_Coord_Axes(const Matrix3D & tm);
		
	void	Render(RenderInfoClass & rinfo);

	void	Set_Next(WidgetRenderOpClass * next)		{ Next = next; }
	WidgetRenderOpClass * Get_Next(void)				{ return Next; }

protected:

	void	render_point(RenderInfoClass & rinfo);
	void	render_vector(RenderInfoClass & rinfo);
	void	render_aabox(RenderInfoClass & rinfo);
	void	render_obbox(RenderInfoClass & rinfo);
	void	render_axes(RenderInfoClass & rinfo);

	enum { RENDER_NONE = 0, RENDER_POINT, RENDER_VECTOR, RENDER_AABOX, RENDER_OBBOX, RENDER_AXES };

	int								RenderOp;
	Vector3							Color;
	float								Opacity;

	Vector3							V0;				// these are all re-used depending on the render op
	Vector3							V1;
	Vector3							V2;
	Matrix3D							Transform;

	WidgetRenderOpClass *		Next;
};

/*
** declare the instance of the pool object for all WidgetRenderOpClass's
*/
DEFINE_AUTO_POOL(WidgetRenderOpClass,256);

/*******************************************
**
** WidgetRenderOpClass Implementation
**
*******************************************/
WidgetRenderOpClass::WidgetRenderOpClass(void) :
	RenderOp(RENDER_NONE),
	Next(NULL)
{
}

void WidgetRenderOpClass::Set_Color(const Vector3 & color)
{
	Color = color;
}

void WidgetRenderOpClass::Set_Opacity(float opacity)
{
	Opacity = opacity;
}

void WidgetRenderOpClass::Init_Point(const Vector3 & p0)
{
	RenderOp = RENDER_POINT;
	V0 = p0;
}

void WidgetRenderOpClass::Init_Vector(const Vector3 & point,const Vector3 & vector)
{
	RenderOp = RENDER_VECTOR;
	V0 = point;
	V1 = vector;
}

void WidgetRenderOpClass::Init_AABox(const AABoxClass & box)
{
	RenderOp = RENDER_AABOX;
	V0 = box.Center;
	V1 = box.Extent;
}

void WidgetRenderOpClass::Init_OBBox(const OBBoxClass & box)
{
	RenderOp = RENDER_OBBOX;
	V0 = Vector3(0,0,0);
	V1 = box.Extent;
	Transform.Set(box.Basis,box.Center);
}

void WidgetRenderOpClass::Init_Coord_Axes(const Matrix3D & tm)
{
	RenderOp = RENDER_AXES;
	Transform = tm;
}

void WidgetRenderOpClass::Render(RenderInfoClass & rinfo)
{
	switch(RenderOp)
	{
	case RENDER_NONE:
		break;
	case RENDER_POINT:
		render_point(rinfo);
		break;
	case RENDER_VECTOR:
		render_vector(rinfo);
		break;
	case RENDER_AABOX:
		render_aabox(rinfo);
		break;
	case RENDER_OBBOX:
		render_obbox(rinfo);
		break;
	case RENDER_AXES:
		render_axes(rinfo);
		break;
	};
}

void WidgetRenderOpClass::render_point(RenderInfoClass & rinfo)
{
}

void WidgetRenderOpClass::render_vector(RenderInfoClass & rinfo)
{
}

void WidgetRenderOpClass::render_aabox(RenderInfoClass & rinfo)
{
}

void WidgetRenderOpClass::render_obbox(RenderInfoClass & rinfo)
{
}

void WidgetRenderOpClass::render_axes(RenderInfoClass & rinfo)
{
}

/*******************************************
**
** WidgetUserClass Implementation
**
*******************************************/
WidgetUserClass::WidgetUserClass(void)
{
}

WidgetUserClass::~WidgetUserClass(void)
{
	Reset_Debug_Widget_List();
}
