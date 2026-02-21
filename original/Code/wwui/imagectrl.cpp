#include "imagectrl.h"
#include "texture.h"
#include "assetmgr.h"
#include "stylemgr.h"


//////////////////////////////////////////////////////////////////////
//
//	ImageCtrlClass
//
//////////////////////////////////////////////////////////////////////
ImageCtrlClass::ImageCtrlClass (void)
{
	//
	//	Configure the renderers
	//
	StyleMgrClass::Configure_Renderer (&ControlRenderer);
	StyleMgrClass::Configure_Renderer (&TextureRenderer);	
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	~ImageCtrlClass
//
//////////////////////////////////////////////////////////////////////
ImageCtrlClass::~ImageCtrlClass (void)
{	
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Create_Control_Renderer
//
//////////////////////////////////////////////////////////////////////
void
ImageCtrlClass::Create_Control_Renderer (void)
{
	Render2DClass &renderer = ControlRenderer;

	//
	//	Configure this renderer
	//
	renderer.Reset ();
	renderer.Enable_Texturing (false);

	//
	//	Determine which color to draw the outline in
	//
	int color	= StyleMgrClass::Get_Line_Color ();
	int bkcolor	= StyleMgrClass::Get_Bk_Color ();
	if (IsEnabled == false) {
		color		= StyleMgrClass::Get_Disabled_Line_Color ();
		bkcolor	= StyleMgrClass::Get_Disabled_Bk_Color ();
	}

	//
	//	Draw the control's outline
	//
	if ( Style & WS_BORDER == WS_BORDER ) {
		renderer.Add_Outline (Rect, 1.0F, color);
	}
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Create_Texture_Renderer
//
//////////////////////////////////////////////////////////////////////
void
ImageCtrlClass::Create_Texture_Renderer (void)
{
	Render2DClass &renderer = TextureRenderer;

	//
	//	Simply draw the texture inside the control
	//
	renderer.Reset ();

	if (renderer.Peek_Texture () != NULL) {
		renderer.Add_Quad (Rect);
	}

	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Set_Texture
//
//////////////////////////////////////////////////////////////////////
void
ImageCtrlClass::Set_Texture (const char *texture_name)
{
	//
	//	Assign the texture to the renderer
	//
	TextureClass *texture = WW3DAssetManager::Get_Instance ()->Get_Texture (texture_name, TextureClass::MIP_LEVELS_1);
	TextureRenderer.Set_Texture (texture);
	REF_PTR_RELEASE (texture);

	//
	//	Force an update
	//
	Set_Dirty ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Render
//
////////////////////////////////////////////////////////////////
void
ImageCtrlClass::Render (void)
{
	//
	//	Recreate the renderers (if necessary)
	//
	if (IsDirty) {
		Create_Control_Renderer ();
		Create_Texture_Renderer ();
	}

	//
	//	Render the image...
	//	
	TextureRenderer.Render ();
	ControlRenderer.Render ();

	DialogControlClass::Render ();
	return ;
}
