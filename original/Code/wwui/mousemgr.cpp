#include "global.h"
#include "mousemgr.h"
#include "screencursor.h"
#include "assetmgr.h"
#include "texture.h"

////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
static const char * TEXTURE_NAME[MouseMgrClass::CURSOR_COUNT] =
{
	"cursor_arrow.tga",
	"cursor_text.tga",
	"cursor_action.tga",
	"cursor_busy.tga",
	"cursor_pan_up.tga",
	"cursor_rotate.tga"
};

static Vector2 HOTSPOTS[MouseMgrClass::CURSOR_COUNT] =
{
	Vector2 (0, 0),
	Vector2 (15, 15),
	Vector2 (7, 1),
	Vector2 (15, 15),
	Vector2 (15, 17),
	Vector2 (15, 15)
};

////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
TextureClass *						MouseMgrClass::Textures[CURSOR_COUNT]	= { 0 };
ScreenCursorClass *				MouseMgrClass::MouseCursor					= NULL;
MouseMgrClass::CURSOR_TYPE		MouseMgrClass::CursorType					= MouseMgrClass::CURSOR_ARROW;
unsigned int MouseMgrClass::CursorWaitCount = 0;

////////////////////////////////////////////////////////////////
//
//	Initialize
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Initialize (void)
{
	//
	//	Load each mouse cursor texture
	//
	for (int index = 0; index < CURSOR_COUNT; index ++) {
		Textures[index] = WW3DAssetManager::Get_Instance()->Get_Texture (TEXTURE_NAME[index], TextureClass::MIP_LEVELS_1);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Shutdown
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Shutdown (void)
{
	//
	//	Free each mouse cursor texture
	//
	for (int index = 0; index < CURSOR_COUNT; index ++) {
		REF_PTR_RELEASE (Textures[index]);
	}

	//
	//	This will free the cursor object
	//
	Show_Cursor (false);
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Show_Cursor
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Show_Cursor (bool onoff)
{
	if (MouseCursor == NULL && onoff) {
		
		//
		//	Create the mouse cursor
		//
		MouseCursor = new ScreenCursorClass;
		MouseCursor->Set_Texture (Textures[CursorType]);
		MouseCursor->Set_Hotspot (HOTSPOTS[CursorType]);
		CursorWaitCount = 0;
		
	} else if (MouseCursor != NULL && onoff == false) {
		
		//
		//	Free the mouse cursor
		//
		delete MouseCursor;
		MouseCursor = NULL;
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Set_Cursor
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Set_Cursor (CURSOR_TYPE type)
{
	if (CursorType != type && MouseCursor != NULL && CursorWaitCount == 0) {
		MouseCursor->Set_Texture (Textures[type]);
		MouseCursor->Set_Hotspot (HOTSPOTS[type]);
		CursorType = type;
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Begin_Wait_Cursor
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Begin_Wait_Cursor(void)
{
	if (CursorWaitCount == 0) {
		Set_Cursor(CURSOR_BUSY);
	}

	++CursorWaitCount;
}

////////////////////////////////////////////////////////////////
//
//	End_Wait_Cursor
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::End_Wait_Cursor(void)
{
	assert(CursorWaitCount > 0 && "End_Wait_Cursor() mismatch");

	if (CursorWaitCount > 0) {
		--CursorWaitCount;

		if (CursorWaitCount == 0) {
			Set_Cursor(CURSOR_ARROW);
		}
	}
}

////////////////////////////////////////////////////////////////
//
//	Render
//
////////////////////////////////////////////////////////////////
void
MouseMgrClass::Render (void)
{
	if (MouseCursor != NULL) {
		MouseCursor->Render ();
	}

	return ;
}
