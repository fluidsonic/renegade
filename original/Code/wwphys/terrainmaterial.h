
#ifndef __TERRAINMATERIAL_H
#define __TERRAINMATERIAL_H

#include "wwstring.h"
#include "refcount.h"

//////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////
class TextureClass;
class ChunkSaveClass;
class ChunkLoadClass;

//////////////////////////////////////////////////////////////////////
//
//	TerrainMaterialClass
//
//////////////////////////////////////////////////////////////////////
class TerrainMaterialClass : public RefCountClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	TerrainMaterialClass  (void);
	virtual ~TerrainMaterialClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public methods
	///////////////////////////////////////////////////////////////////

	//
	//	Texture support
	//
	TextureClass *			Peek_Texture (void)							{ return Texture; }
	const char *			Get_Texture_Name (void)						{ return TextureName; }
	void						Set_Texture (const char *texture_name);
	
	//
	//	UV mapping control
	//
	void						Set_Meters_Per_Tile (float value)		{ MetersPerTile = value; }
	float						Get_Meters_Per_Tile (void) const			{ return MetersPerTile; }
	
	void						Mirror_UVs (bool onoff)						{ AreUVsMirrored = onoff; }
	bool						Are_UVs_Mirrored (void) const				{ return AreUVsMirrored; }	

	//
	//	Surface type support
	//
	void						Set_Surface_Type (int type);
	int						Get_Surface_Type (void)				{ return SurfaceType; }

	//
	//	Save/load support
	//
	bool						Save (ChunkSaveClass &csave);
	bool						Load (ChunkLoadClass &cload);
	void						Load_Variables (ChunkLoadClass &cload);
	
protected:
	
	///////////////////////////////////////////////////////////////////
	//	Protected methods
	///////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////
	//	Protected member data
	///////////////////////////////////////////////////////////////////
	StringClass				TextureName;
	TextureClass *			Texture;
	float						MetersPerTile;
	bool						AreUVsMirrored;
	int						SurfaceType;
};

#endif //__TERRAINMATERIAL_H
