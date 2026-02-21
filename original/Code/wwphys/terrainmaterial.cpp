#include "terrainmaterial.h"
#include "texture.h"
#include "assetmgr.h"
#include "chunkio.h"
#include "w3d_file.h"


////////////////////////////////////////////////////////////////
//	Local constants
////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_VARIABLES				= 0x02261040,
};

enum
{
	VARID_XXXXXXX_XXXX			= 0x01,
	VARID_METERS_PER_TILE,
	VARID_TEXTURE_NAME,
	VARID_MIRRORED_UVS,
	VARID_SURFACE_TYPE,
};


//////////////////////////////////////////////////////////////////////
//
//	TerrainMaterialClass
//
//////////////////////////////////////////////////////////////////////
TerrainMaterialClass::TerrainMaterialClass (void)	:
	Texture (NULL),
	MetersPerTile (10.0F),
	SurfaceType (0),
	AreUVsMirrored (false)
{
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	~TerrainMaterialClass
//
//////////////////////////////////////////////////////////////////////
TerrainMaterialClass::~TerrainMaterialClass (void)
{
	//
	//	Release the texture
	//
	REF_PTR_RELEASE (Texture);
	return ;
}


//////////////////////////////////////////////////////////////////////
//
//	Set_Texture
//
//////////////////////////////////////////////////////////////////////
void
TerrainMaterialClass::Set_Texture (const char *texture_name)
{
	//
	//	Release the old texture
	//
	REF_PTR_RELEASE (Texture);
	
	//
	//	Load the new texture
	//
	TextureName = texture_name;

#ifndef PARAM_EDITING_ON
	const char *dir_delim = ::strrchr (texture_name, '\\');
	if (dir_delim != NULL) {
		TextureName = dir_delim + 1;
	}
#endif

	Texture = WW3DAssetManager::Get_Instance ()->Get_Texture (TextureName);
	return ;
}

//////////////////////////////////////////////////////////////////////
//
//	Save
//
//////////////////////////////////////////////////////////////////////
void
TerrainMaterialClass::Set_Surface_Type (int type)		
{ 
	if ((type >= 0) && (type < SURFACE_TYPE_MAX)) {
		SurfaceType = type; 
	}
}


//////////////////////////////////////////////////////////////////////
//
//	Save
//
//////////////////////////////////////////////////////////////////////
bool
TerrainMaterialClass::Save (ChunkSaveClass &csave)
{
	//
	//	Write the variables
	//
	csave.Begin_Chunk (CHUNKID_VARIABLES);
		WRITE_MICRO_CHUNK_WWSTRING	(csave, VARID_TEXTURE_NAME,		TextureName);
		WRITE_MICRO_CHUNK				(csave, VARID_METERS_PER_TILE,	MetersPerTile);
		WRITE_MICRO_CHUNK				(csave, VARID_MIRRORED_UVS,		AreUVsMirrored);
		WRITE_MICRO_CHUNK				(csave, VARID_SURFACE_TYPE,		SurfaceType);
	csave.End_Chunk ();

	return true;
}


//////////////////////////////////////////////////////////////////////
//
//	Load
//
//////////////////////////////////////////////////////////////////////
bool
TerrainMaterialClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {
		switch (cload.Cur_Chunk_ID ()) {
			
			//
			//	Load all the variables from this chunk
			//
			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;
		}

		cload.Close_Chunk ();
	}

	//
	//	Load the texture into memory
	//
	Set_Texture (TextureName);
	return true;
}


////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
////////////////////////////////////////////////////////////////
void
TerrainMaterialClass::Load_Variables (ChunkLoadClass &cload)
{
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {

			//
			//	Read each of the microchunks
			//
			READ_MICRO_CHUNK_WWSTRING	(cload, VARID_TEXTURE_NAME,		TextureName);
			READ_MICRO_CHUNK				(cload, VARID_METERS_PER_TILE,	MetersPerTile);
			READ_MICRO_CHUNK				(cload, VARID_MIRRORED_UVS,		AreUVsMirrored);
			READ_MICRO_CHUNK				(cload, VARID_SURFACE_TYPE,		SurfaceType);			
		}

		cload.Close_Micro_Chunk ();
	}

	//
	// Enforcing that the surface type is valid; the game will assert this later otherwise...
	// Also fixed it on the editor side, surface types are initialized to zero.
	//
	if ((SurfaceType < 0) || (SurfaceType >= SURFACE_TYPE_MAX)) {
		SurfaceType = 0;
	}

	return ;
}
