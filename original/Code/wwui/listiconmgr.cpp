#include "listiconmgr.h"
#include "stylemgr.h"
#include "assetmgr.h"
#include "texture.h"
#include "render2d.h"


////////////////////////////////////////////////////////////////
//
//	ListIconMgrClass
//
////////////////////////////////////////////////////////////////
ListIconMgrClass::ListIconMgrClass (void)	:
	IconWidth (16),
	IconHeight (16)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Add_Icon
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Add_Icon (const char *texture_name)
{
	//
	//	Make sure this texture isn't already in our lists
	//
	int index = Find_Texture (texture_name);
	if (index == -1) {

		//
		//	Load the texture
		//
		TextureClass *texture = WW3DAssetManager::Get_Instance ()->Get_Texture (texture_name, TextureClass::MIP_LEVELS_1);	
		if (texture != NULL) {

			//
			//	Create a new renderer and assign it the texture
			//
			Render2DClass *renderer = new Render2DClass;
			StyleMgrClass::Configure_Renderer (renderer);
			renderer->Set_Texture (texture);
			renderer->Enable_Alpha (true);

			//
			//	Add this renderer to our list
			//
			Renderers.Add (renderer);
			TextureNames.Add (texture_name);

			//
			//	Release our hold on the texture
			//
			REF_PTR_RELEASE (texture);
		}
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Remove_Icon
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Remove_Icon (const char *texture_name)
{
	//
	//	Try to find the texture in our list
	//
	int index = Find_Texture (texture_name);
	if (index >= 0) {

		//
		//	Remove this entry from out lists
		//
		delete Renderers[index];
		Renderers.Delete (index);
		TextureNames.Delete (index);
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Reset_Icons
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Reset_Icons (void)
{
	//
	//	First, free each renderer
	//
	for (int index = 0; index < Renderers.Count (); index ++) {
		delete Renderers[index];
	}

	//
	//	Now, reset the lists
	//
	Renderers.Delete_All ();
	TextureNames.Delete_All ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Render_Icon
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Render_Icon (const RectClass &clip_rect, const char *texture_name)
{
	//
	//	Try to find the texture in our list
	//
	int index = Find_Texture (texture_name);
	if (index >= 0) {

		float icon_size_x = IconWidth * StyleMgrClass::Get_X_Scale ();
		float icon_size_y = IconHeight * StyleMgrClass::Get_Y_Scale ();

		//
		//	Render the texture left-aligned and v-centered in the clip rectangle
		//
		RectClass rect;
		rect.Left			= clip_rect.Left;
		rect.Right			= clip_rect.Left + icon_size_x;
		rect.Top				= int (clip_rect.Center ().Y - (icon_size_y * 0.5F));
		rect.Bottom			= int (clip_rect.Center ().Y + (icon_size_y * 0.5F));
		Renderers[index]->Add_Quad (rect);
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Render_Icons
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Render_Icons (void)
{
	for (int index = 0; index < Renderers.Count (); index ++) {
		Renderers[index]->Render ();
	}

	return ;
}


////////////////////////////////////////////////////////////////
//
//	Find_Texture
//
////////////////////////////////////////////////////////////////
int
ListIconMgrClass::Find_Texture (const char *texture_name)
{
	int retval = -1;

	//
	//	Try to find a texture with this name in our list
	//
	for (int index = 0; index < TextureNames.Count (); index ++) {
		if (::lstrcmpi (texture_name, TextureNames[index]) == 0) {
			retval = index;
			break;
		}
	}

	return retval;
}


////////////////////////////////////////////////////////////////
//
//	Reset_Renderers
//
////////////////////////////////////////////////////////////////
void
ListIconMgrClass::Reset_Renderers (void)
{
	for (int index = 0; index < Renderers.Count (); index ++) {
		Renderers[index]->Reset ();
	}

	return ;
}
