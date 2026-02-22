#pragma once

#include "global.h"

#include "shader.h"

class TextureClass;
class VertexMaterialClass;
class MaterialPassClass;

/**
** PhysResourceMgrClass
** This class is just a collection of resources that I've ended up needed at one time or another
** in the physics library.  I thought it would be easier to collect them all here so the Init and 
** Shutdown code can clean them all up at once.  
*/
class PhysResourceMgrClass
{
public:

	static void Init(void);
	static void Shutdown(void);

	static bool								Set_Shadow_Blob_Texture(const char * texname);	
	static TextureClass *				Get_Shadow_Blob_Texture(void);						

	static MaterialPassClass *			Get_Highlight_Material_Pass(void);

	static TextureClass *				Get_Stealth_Texture(void);
	static TextureClass *				Peek_Stealth_Texture(void);

	static VertexMaterialClass *		Create_Emissive_Material(void);

	static TextureClass *				Get_Grid_Texture(void);
	static TextureClass *				Peek_Grid_Texture(void);
};
