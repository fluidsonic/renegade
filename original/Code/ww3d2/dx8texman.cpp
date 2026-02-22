// This class manages textures that are in the default pool
// ensuring that they are released on device loss
// and created on device reset

// Note: It does NOT addref to textures because it is called in the texture
// destructor

#include "global.h"
#include "dx8texman.h"

DX8TextureTrackerList DX8TextureManagerClass::Managed_Textures;

/***********************************************************************************************
 * DX8TextureManagerClass::Shutdown -- Shuts down the texture manager                          *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   4/25/2001  hy : Created.                                                                  *
 *=============================================================================================*/
void DX8TextureManagerClass::Shutdown()
{
	while (!Managed_Textures.Is_Empty())
	{
		DX8TextureTrackerClass *track=Managed_Textures.Remove_Head();
		delete track;
		track=NULL;
	}
}

/***********************************************************************************************
 * DX8TextureManagerClass::Add -- Adds a texture to be managed                                 *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   4/25/2001  hy : Created.                                                                  *
 *=============================================================================================*/
void DX8TextureManagerClass::Add(DX8TextureTrackerClass *track)
{
	// this function should only be called by the texture constructor
	Managed_Textures.Add(track);
}

/***********************************************************************************************
 * DX8TextureManagerClass::Remove -- Removes a texture from being managed                      *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   4/25/2001  hy : Created.                                                                  *
 *=============================================================================================*/
void DX8TextureManagerClass::Remove(TextureClass *tex)
{
	// this function should only be called by the texture destructor
	DX8TextureTrackerListIterator it(&Managed_Textures);

	while (!it.Is_Done())
	{
		DX8TextureTrackerClass *track=it.Peek_Obj();		
		if (track->Texture==tex)
		{			
			it.Remove_Current_Object();
			delete track;
			break;
		}
		it.Next();
	}
}

/***********************************************************************************************
 * DX8TextureManagerClass::Release_Textures -- Releases the internal d3d texture               *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   4/25/2001  hy : Created.                                                                  *
 *=============================================================================================*/
void DX8TextureManagerClass::Release_Textures()
{
	DX8TextureTrackerListIterator it(&Managed_Textures);

	while (!it.Is_Done())
	{
		DX8TextureTrackerClass *track=it.Peek_Obj();		
		track->Texture->D3DTexture->Release();
		track->Texture->D3DTexture=NULL;
		it.Next();
	}
}

/***********************************************************************************************
 * DX8TextureManagerClass::Recreate_Textures -- Reallocates lost textures                      *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   4/25/2001  hy : Created.                                                                  *
 *=============================================================================================*/
void DX8TextureManagerClass::Recreate_Textures()
{
	DX8TextureTrackerListIterator it(&Managed_Textures);

	while (!it.Is_Done())
	{
		DX8TextureTrackerClass *track=it.Peek_Obj();
		track->Texture->D3DTexture=DX8Wrapper::_Create_DX8_Texture(track->Width,track->Height,
			track->Format,track->Mip_level_count,D3DPOOL_DEFAULT,track->RenderTarget);
		track->Texture->Dirty=true;
		it.Next();
	}
}
