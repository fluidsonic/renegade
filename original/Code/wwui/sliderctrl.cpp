// Disable warning about exception handling not being enabled. It's used as part of STL - in a part of STL we don't use.

#include "global.h"
#include "sliderctrl.h"
#include "assetmgr.h"
#include "refcount.h"
#include "mousemgr.h"
#include "ww3d.h"
#include "dialogmgr.h"
#include "dialogbase.h"
#include "stylemgr.h"

////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
const float BAR_HEIGHT	= 8.0F;

////////////////////////////////////////////////////////////////
//
//	SliderCtrlClass
//
////////////////////////////////////////////////////////////////
SliderCtrlClass::SliderCtrlClass (void)	:
	MinPos (0),
	MaxPos (100),
	CurrPos (0),
	IsDragging (false)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	~SliderCtrlClass
//
////////////////////////////////////////////////////////////////
SliderCtrlClass::~SliderCtrlClass (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Create_Control_Renderer
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::Create_Control_Renderer (void)
{
	Render2DClass &renderer = ControlRenderer;

	//
	//	Configure this renderer
	//
	renderer.Reset ();
	renderer.Enable_Texturing (false);
	renderer.Set_Coordinate_Range (Render2DClass::Get_Screen_Resolution());

	//
	//	Determine which color to draw the outline in
	//
	int color		= StyleMgrClass::Get_Line_Color ();
	int bkcolor		= StyleMgrClass::Get_Bk_Color ();
	if (IsEnabled == false) {
		color		= StyleMgrClass::Get_Disabled_Line_Color ();
		bkcolor	= StyleMgrClass::Get_Disabled_Bk_Color ();
	}

	//
	//	Calculate the thumb's position
	//
	float percent		= float(CurrPos - MinPos) / float(MaxPos - MinPos);
	int thumb_height	= ClientRect.Height () - 2;
	int thumb_width	= thumb_height / 2;

	//
	//	Calculate the rectangle for the thumb
	//
	RectClass rect;
	rect.Left	= int (ClientRect.Left + (ClientRect.Width () * percent) - (thumb_width * 0.5F));
	rect.Right	= rect.Left + thumb_width;
	rect.Top		= int(ClientRect.Top + (ClientRect.Height () / 2) - (thumb_height / 2));
	rect.Bottom	= int(ClientRect.Top + (ClientRect.Height () / 2) + (thumb_height / 2));

	//
	//	Draw the thumb
	//
	renderer.Add_Rect (rect, 1.0F, color, HasFocus ? color : bkcolor);

	//
	//	Determine what height to use for the bar
	//
	float bar_height = BAR_HEIGHT * StyleMgrClass::Get_Y_Scale ();

	//
	//	Calculate the rectangle for the control
	//	
	RectClass rect1;
	RectClass rect2;
	rect1.Left		= ClientRect.Left;
	rect1.Right		= rect.Left;
	rect2.Left		= rect.Right;
	rect2.Right		= ClientRect.Right;
	rect1.Top		= int(ClientRect.Top + (ClientRect.Height () / 2) - (bar_height / 2));
	rect1.Bottom	= int(ClientRect.Top + (ClientRect.Height () / 2) + (bar_height / 2));
	rect2.Top		= rect1.Top;
	rect2.Bottom	= rect1.Bottom;

	//
	//	Draw the bar in two sections
	//
	if (rect1.Right > rect1.Left) {
		renderer.Add_Rect (rect1, 1.0F, color, bkcolor);
	}

	if (rect2.Right > rect2.Left) {
		renderer.Add_Rect (rect2, 1.0F, color, bkcolor);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Set_Cursor
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_Set_Cursor (const Vector2 &mouse_pos)
{
	//
	//	Change the mouse cursor
	//
	MouseMgrClass::Set_Cursor (MouseMgrClass::CURSOR_ACTION);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Update_Client_Rect
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::Update_Client_Rect (void)
{
	//
	//	Set the client area
	//
	ClientRect = Rect;
	Set_Dirty ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Render
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::Render (void)
{
	//
	//	Recreate the renderers (if necessary)
	//
	if (IsDirty) {
		Create_Control_Renderer ();
	}

	//
	//	Render the background and text for the current state
	//
	ControlRenderer.Render ();

	DialogControlClass::Render ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_LButton_Down
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_LButton_Down (const Vector2 &mouse_pos)
{
	Set_Capture ();
	IsDragging = true;

	//
	//	Snap the slider to the mouse position
	//
	Set_Pos (Slider_Pos_From_Mouse_Pos (mouse_pos));	
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_LButton_Up
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_LButton_Up (const Vector2 &mouse_pos)
{
	Release_Capture ();
	IsDragging = false;
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Mouse_Move
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_Mouse_Move (const Vector2 &mouse_pos)
{
	if (IsDragging) {
		Set_Pos (Slider_Pos_From_Mouse_Pos (mouse_pos));
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Set_Focus
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_Set_Focus (void)
{
	Set_Dirty ();

	DialogControlClass::On_Set_Focus ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Kill_Focus
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_Kill_Focus (DialogControlClass *focus)
{
	IsDragging = false;
	Set_Dirty ();	

	DialogControlClass::On_Kill_Focus (focus);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Key_Down
//
////////////////////////////////////////////////////////////////
bool
SliderCtrlClass::On_Key_Down (uint32_t key_id, uint32_t key_data)
{
	bool handled = true;

	switch (key_id)
	{
		case VK_DOWN:
		case VK_LEFT:
			Set_Pos (CurrPos - 1);
			break;

		case VK_UP:
		case VK_RIGHT:
			Set_Pos (CurrPos + 1);
			break;

		case VK_HOME:
			Set_Pos (MinPos);
			break;

		case VK_END:
			Set_Pos (MaxPos);
			break;

		default:
			handled = false;
			break;
	}

	return handled;
}

////////////////////////////////////////////////////////////////
//
//	On_Create
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::On_Create (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Slider_Pos_From_Mouse_Pos
//
////////////////////////////////////////////////////////////////
int
SliderCtrlClass::Slider_Pos_From_Mouse_Pos (const Vector2 &mouse_pos)
{
	int retval = 0;
	
	if (mouse_pos.X < ClientRect.Left) {
		
		//
		// Mouse is to the left of the control
		//
		retval = MinPos;
	} else if (mouse_pos.X >= ClientRect.Right) {
		
		//
		//	Mouse is to the right of the control
		//
		retval = MaxPos;
	} else {

		int thumb_height	= ClientRect.Height () - 2;
		int thumb_width	= thumb_height / 2;

		//
		//	Calculate where in the range the mouse is and return its position
		//
		float percent = (mouse_pos.X - (ClientRect.Left - (thumb_width * 0.5F))) / ClientRect.Width ();
		retval = MinPos + int(percent * (MaxPos - MinPos)) ;
	}

	return retval;
}

////////////////////////////////////////////////////////////////
//
//	Set_Range
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::Set_Range (int range_min, int range_max)
{
	MinPos = range_min;
	MaxPos = range_max;
	Set_Pos (MinPos);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Set_Pos
//
////////////////////////////////////////////////////////////////
void
SliderCtrlClass::Set_Pos (int pos, bool send_notification)
{
	//
	//	Bound the new position
	//
	if (pos < MinPos) {
		pos = MinPos;
	} else if (pos > MaxPos) {
		pos = MaxPos;
	}

	//
	//	Update the current position
	//
	if (pos != CurrPos) {
		CurrPos = pos;
		Set_Dirty ();

		//
		//	Notify the advise sinks if necessary
		//
		if (send_notification) {
			ADVISE_NOTIFY (On_SliderCtrl_Pos_Changed (this, Get_ID (), CurrPos));
		}		
	}

	return ;
}
