#if defined(_MSC_VER)
#pragma once
#endif

#ifndef ENBASSETMGR_H
#define ENBASSETMGR_H

#include <assetmgr.h>

class Vector3;
class VertexMaterialClass;

class ENBAssetManager: public WW3DAssetManager
{
public:
	ENBAssetManager(void);
	virtual ~ENBAssetManager(void);	

	// unique to ENBAssetManager
	virtual RenderObjClass * Create_Render_Obj(const char * name,float Scale,Vector3 &HSV_shift);	

private:
	void Make_Mesh_Unique(RenderObjClass *robj,bool geometry, bool colors);
	void Make_HLOD_Unique(RenderObjClass *robj,bool geometry, bool colors);
	void Make_Unique(RenderObjClass *robj,bool geometry, bool colors);
	void Recolor_Vertex_Material(VertexMaterialClass *vmat,Vector3 &hsv_shift);
	void Recolor_Vertices(unsigned int *color, int count, Vector3 &hsv_shift);	
	void Recolor_Mesh(RenderObjClass *robj,Vector3 &hsv_shift);
	TextureClass * Recolor_Texture(TextureClass *texture, Vector3 &hsv_shift);
	void Recolor_HLOD(RenderObjClass *robj,Vector3 &hsv_shift);
	void Recolor_ParticleEmitter(RenderObjClass *robj,Vector3 &hsv_shift);
	void Recolor_Asset(RenderObjClass *robj,Vector3 &hsv_shift);
};

#endif

