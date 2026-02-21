
#ifndef __RENEGADETERRAINMATERIALPASS_H
#define __RENEGADETERRAINMATERIALPASS_H

#include "vector.h"


//////////////////////////////////////////////////////////////////////
//	Forward declarations
//////////////////////////////////////////////////////////////////////
class Vector2;
class TerrainMaterialClass;
class DX8IndexBufferClass;
class DX8VertexBufferClass;
class ChunkSaveClass;
class ChunkLoadClass;


//////////////////////////////////////////////////////////////////////
//
//	RenegadeTerrainMaterialPassClass
//
//////////////////////////////////////////////////////////////////////
class RenegadeTerrainMaterialPassClass
{
public:

	///////////////////////////////////////////////////////////////////
	//	Public data types
	///////////////////////////////////////////////////////////////////
	typedef enum
	{
		PASS_BASE			= 0,
		PASS_ALPHA,
		PASS_COUNT

	} TEXTURE_PASS_TYPE;	

	///////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	///////////////////////////////////////////////////////////////////
	RenegadeTerrainMaterialPassClass  (void);
	virtual ~RenegadeTerrainMaterialPassClass (void);

	///////////////////////////////////////////////////////////////////
	//	Public Methods
	///////////////////////////////////////////////////////////////////
	void		Allocate (int vertex_count);
	void		Reset (void);

	//
	//	Save/load support
	//
	bool		Save (ChunkSaveClass &csave);
	bool		Load (ChunkLoadClass &cload);
	void		Load_Variables (ChunkLoadClass &cload);

	///////////////////////////////////////////////////////////////////
	//	Public member data
	///////////////////////////////////////////////////////////////////
	int								VertexCount;
	float *							VertexAlpha;
	Vector2 *						GridUVs;
	TerrainMaterialClass *		Material;
	DynamicVectorClass<int>		QuadList[PASS_COUNT];
	DynamicVectorClass<int>		VertexRenderList[PASS_COUNT];
	int *								VertexIndexMap[PASS_COUNT];

	DX8IndexBufferClass *		IndexBuffers[PASS_COUNT];
	DX8VertexBufferClass *		VertexBuffers[PASS_COUNT];
};


#endif //__RENEGADETERRAINMATERIALPASS_H
