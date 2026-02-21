// Disable warning about exception handling not being enabled. It's used as part of STL - in a part of STL we don't use.
#pragma warning(disable : 4530)

#include "screencursor.h"
#include "ww3d.h"
#include "texture.h"
#include "render2d.h"
#include "rect.h"
#include "dialogmgr.h"


///////////////////////////////////////////////////////////////////
//
//	ScreenCursorClass
//
///////////////////////////////////////////////////////////////////
ScreenCursorClass::ScreenCursorClass (void)	:
	Hotspot (0, 0),
	Texture (NULL),
	Width (0),
	Height (0)
{
	return ;
}


///////////////////////////////////////////////////////////////////
//
//	~ScreenCursorClass
//
///////////////////////////////////////////////////////////////////
ScreenCursorClass::~ScreenCursorClass (void)
{
	REF_PTR_RELEASE (Texture);
	return ;
}


///////////////////////////////////////////////////////////////////
//
//	Set_Texture
//
///////////////////////////////////////////////////////////////////
void
ScreenCursorClass::Set_Texture (TextureClass *texture)
{
	REF_PTR_SET (Texture, texture);

	//
	// Find the dimensions of the texture:
	//
	if (Texture != NULL) {
//		SurfaceClass::SurfaceDescription surface_desc;
//		Texture->Get_Level_Description(surface_desc);
//		Width	= surface_desc.Width;
//		Height	= surface_desc.Height;
		Texture->Init();
		Width = Texture->Get_Width();
		Height = Texture->Get_Height();
	}

	Renderer.Set_Texture (Texture);
	Renderer.Set_Coordinate_Range (Render2DClass::Get_Screen_Resolution ());
	return ;
}


///////////////////////////////////////////////////////////////////
//
//	Render
//
///////////////////////////////////////////////////////////////////
void
ScreenCursorClass::Render (void)
{
	//
	//	Get the cursor's position
	//
	Vector3 cursor_pos = DialogMgrClass::Get_Mouse_Pos ();

	//
	//	Clamp the cursor to the screen bounds
	//
	const RectClass &screen_rect = Render2DClass::Get_Screen_Resolution ();
	cursor_pos.X = WWMath::Clamp (cursor_pos.X, screen_rect.Left, screen_rect.Right - 5);
	cursor_pos.Y = WWMath::Clamp (cursor_pos.Y, screen_rect.Top, screen_rect.Bottom - 5);
	DialogMgrClass::Set_Mouse_Pos (cursor_pos);

	//
	//	Take the hotspot into account
	//
	cursor_pos.X -= Hotspot.X;
	cursor_pos.Y -= Hotspot.Y;

	//
	//	Build the screen-space rectangle we'll render to
	//
	RectClass rect;
	rect.Left	= (int)cursor_pos.X;
	rect.Top		= (int)cursor_pos.Y;
	rect.Right	= (int)cursor_pos.X + Width;
	rect.Bottom	= (int)cursor_pos.Y + Height;

	//
	//	Render the mouse cursor
	//
	Renderer.Reset ();
	Renderer.Add_Quad (rect, RectClass (0, 0, 1, 1));
	Renderer.Render ();
	return ;
}

