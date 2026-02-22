#pragma once

#include "global.h"

#include "widestring.h"
#include "vector.h"

////////////////////////////////////////////////////////////////
//	Forward declarations
////////////////////////////////////////////////////////////////
class TextureClass;
class Render2DClass;
class Render2DSentenceClass;
class FontCharsClass;
class RectClass;

////////////////////////////////////////////////////////////////
//
//	StyleMgrClass
//
////////////////////////////////////////////////////////////////
class StyleMgrClass
{
public:

	////////////////////////////////////////////////////////////////
	//	Public constants
	////////////////////////////////////////////////////////////////

	typedef enum
	{
		FONT_TITLE			= 0,
		FONT_LG_CONTROLS,
		FONT_CONTROLS,
		FONT_LISTS,
		FONT_TOOLTIPS,
		FONT_MENU,
		FONT_SM_MENU,
		FONT_HEADER,
		FONT_BIG_HEADER,
		FONT_CREDITS,
		FONT_CREDITS_BOLD,
		FONT_INGAME_TXT,
		FONT_INGAME_BIG_TXT,
		FONT_INGAME_SUBTITLE_TXT,
		FONT_INGAME_HEADER_TXT,
		FONT_MAX
	} FONT_STYLE;

	typedef enum
	{
		LEFT_JUSTIFY		= 0,
		RIGHT_JUSTIFY,
		CENTER_JUSTIFY

	} JUSTIFICATION;

	typedef enum
	{
		EVENT_MOUSE_CLICK		= 0,
		EVENT_MOUSE_OVER,
		EVENT_MENU_BACK,
		EVENT_POPUP,
		EVENT_AUDIO_MAX

	} EVENT_AUDIO;

	////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////

	//
	//	Initialization
	//
	static void				Initialize (void);
	static void				Initialize_From_INI (const char *filename);
	static void				Shutdown (void);

	//
	//	Font methods
	//
	static FontCharsClass *			Get_Font (FONT_STYLE style);
	static FontCharsClass *			Peek_Font (FONT_STYLE style)	{ return Fonts[style]; }
	static void							Assign_Font (Render2DSentenceClass *renderer, FONT_STYLE style);

	//
	//	Sound methods
	//
	static void							Play_Sound (EVENT_AUDIO event);

	//
	//	Configuration methods
	//
	static void				Configure_Renderer (Render2DClass *renderer);

	//
	//	Scale support
	//
	static float			Get_X_Scale (void)	{ return ScaleX; }
	static float			Get_Y_Scale (void)	{ return ScaleY; }

	//
	//	Color methods
	//
	static uint32_t			Get_Text_Color (void)						{ return TextColor; }
	static uint32_t			Get_Text_Shadow_Color (void)				{ return TextShadowColor; }
	static uint32_t			Get_Disabled_Text_Color (void)			{ return DisabledTextColor; }
	static uint32_t			Get_Disabled_Text_Shadow_Color (void)	{ return DisabledTextShadowColor; }
	static uint32_t			Get_Line_Color (void)						{ return LineColor; }
	static uint32_t			Get_Bk_Color (void)							{ return BkColor; }
	static uint32_t			Get_Disabled_Line_Color (void)			{ return DisabledLineColor; }
	static uint32_t			Get_Disabled_Bk_Color (void)				{ return DisabledBkColor; }
	static uint32_t			Get_Tab_Text_Color (void)					{ return TabTextColor; }
	static uint32_t			Get_Tab_Glow_Color (void)					{ return TabGlowColor; }

	//
	//	Backdrop support
	//
	static void				Render_Backdrop (Render2DClass *renderer, const RectClass &rect);
	
	//
	//	Text support
	//
	static void				Render_Text (const WCHAR *text, Render2DSentenceClass *renderer, uint32_t text_color, uint32_t shadow_color, const RectClass &rect, bool do_shadow = false, bool do_clip = true, JUSTIFICATION justify = LEFT_JUSTIFY, bool is_vcentered = true);
	static void				Render_Text (const WCHAR *text, Render2DSentenceClass *renderer, const RectClass &rect, bool do_shadow = false, bool do_clip = true, JUSTIFICATION justify = LEFT_JUSTIFY, bool is_enabled = true, bool is_vcentered = true);
	static void				Render_Title_Text (const WCHAR *text, Render2DSentenceClass *renderer, const RectClass &rect);
	static void				Render_Wrapped_Text (const WCHAR *text, Render2DSentenceClass *renderer, uint32_t text_color, uint32_t shadow_color, const RectClass &rect, bool do_shadow = false, bool do_vcenter = false);
	static void				Render_Wrapped_Text (const WCHAR *text, Render2DSentenceClass *renderer, const RectClass &rect, bool do_shadow = false, bool do_vcenter = false, bool is_enabled = true);	
	static void				Render_Wrapped_Text_Ex (const WCHAR *text, Render2DSentenceClass *renderer, const RectClass &rect, bool do_shadow = false, bool do_vcenter = false, bool is_enabled = true, JUSTIFICATION justify = LEFT_JUSTIFY);
	static void				Render_Wrapped_Text_Ex (const WCHAR *text, Render2DSentenceClass *renderer, uint32_t text_color, uint32_t shadow_color, const RectClass &rect, bool do_shadow = false, bool do_vcenter = false, JUSTIFICATION justify = LEFT_JUSTIFY);

	//
	//	Hilight support
	//
	static void				Configure_Hilighter (Render2DClass *renderer);
	static void				Render_Hilight (Render2DClass *renderer, const RectClass &rect);

	//
	//	Text "glow" support
	//
	static void				Render_Glow (const WCHAR *text, Render2DSentenceClass *renderer, const RectClass &rect, int radius_x, int radius_y, int color, JUSTIFICATION justify = LEFT_JUSTIFY);

private:

	////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////	
	static TextureClass *			BackdropTexture;	
	static uint32_t						TitleColor;
	static uint32_t						TitleHilightColor;
	static uint32_t						TitleShadowColor;
	static uint32_t						TextColor;
	static uint32_t						TextShadowColor;
	static uint32_t						LineColor;
	static uint32_t						BkColor;
	static uint32_t						DisabledTextColor;
	static uint32_t						DisabledTextShadowColor;
	static uint32_t						DisabledLineColor;
	static uint32_t						DisabledBkColor;
	static uint32_t						HilightColor;
	static uint32_t						TabTextColor;
	static uint32_t						TabGlowColor;
	static FontCharsClass *			Fonts[FONT_MAX];
	static float						ScaleX;
	static float						ScaleY;
	static DynamicVectorClass<StringClass>	FontFileList;
	static StringClass				EventAudioList[EVENT_AUDIO_MAX];
};
