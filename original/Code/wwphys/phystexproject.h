
#ifndef PHYSTEXPROJECT_H
#define PHYSTEXPROJECT_H

#include "always.h"
#include "texproject.h"

class PhysClass;

/**
** PhysTexProjectClass
** This derived TexProjectClass simply adds interfaces for handling PhysClasses direction.
** It also handles getting the shadow render context from the physics scene.  
** TODO: should we "standardize" the shadow render context stuff and move it to WW3D?
*/
class PhysTexProjectClass : public TexProjectClass
{
public:

	PhysTexProjectClass(void);
	virtual ~PhysTexProjectClass(void);

	/*
	** Automatic initialization of a TexProjectClass.
	** First set up your projection parameters with one of the Compute_xxx_Projection functinos, 
	** Then call Compute_Texture.
	*/
	bool						Compute_Perspective_Projection(PhysClass * obj,const Vector3 & lightpos,float nearz=-1.0f,float farz=-1.0f);
	bool						Compute_Ortho_Projection(PhysClass * obj,const Vector3 & lightdir,float nearz=-1.0f,float farz=-1.0f);
	bool						Compute_Texture(PhysClass * obj,bool additive_projection = false);


};


#endif

