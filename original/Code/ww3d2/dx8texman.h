
#ifndef DX8TEXTUREMANAGER_H
#define DX8TEXTUREMANAGER_H

#include "always.h"
#include "texture.h"
#include "dx8wrapper.h"
#include "ww3dformat.h"
#include "dx8list.h"
#include "ww3dformat.h"
#include "multilist.h"

class DX8TextureManagerClass;

class DX8TextureTrackerClass : public MultiListObjectClass
{
friend DX8TextureManagerClass;
public:
	DX8TextureTrackerClass(unsigned int w, unsigned int h, WW3DFormat format,
		TextureClass::MipCountType count,bool rt,
		TextureClass *tex):
	Width(w),
	Height(h),
	Format(format),
	Mip_level_count(count),
	RenderTarget(rt),
	Texture(tex)	
	{
	}
private:
	unsigned int Width;
	unsigned int Height;
	WW3DFormat Format;
	TextureClass::MipCountType Mip_level_count;
	bool RenderTarget;
	TextureClass *Texture;
};

class DX8TextureManagerClass
{
public:
	static void Shutdown();
	static void Add(DX8TextureTrackerClass *track);
	static void Remove(TextureClass *tex);
	static void Release_Textures();
	static void Recreate_Textures();
private:
	static DX8TextureTrackerList Managed_Textures;
};

#endif // ifdef TEXTUREMANAGER