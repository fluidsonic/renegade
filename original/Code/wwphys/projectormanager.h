#pragma once

#include "global.h"

#include "wwstring.h"

class RenderObjClass;
class TexProjectClass;
class ChunkLoadClass;
class ChunkSaveClass;

class ProjectorManagerDefClass;

/**
** ProjectorManagerClass
** This class is meant to be embedded in a phyisics object and simply manages the 
** chore of updating the transforms for texture projectors which are attached
** to bones in a model
*/
class ProjectorManagerClass
{
public:
	ProjectorManagerClass(void);
	~ProjectorManagerClass(void);
	
	enum 
	{
		IS_ANIMATED = 0x00000001,
	};

	void											Init(const ProjectorManagerDefClass & def,RenderObjClass * model);
	void											Update_From_Model(RenderObjClass * model);

	void											Set_Flag(int flag,bool onoff)		{ (onoff ? Flags |= flag : Flags &= ~flag); }
	bool											Get_Flag(int flag)					{ return ((Flags & flag) == flag); }

protected:
	
	void											Free(void);

	uint16_t										Flags;
	uint16_t										ProjectorBoneIndex;
	TexProjectClass *							Projector;

};

/**
** ProjectorManagerDefClass
** This class is meant to be a component of a definition class for a physics object
** which has a ProjectorManager.  The member variables of this class are public since
** various physics def classes make them directly editable.
*/
class ProjectorManagerDefClass
{
public:

	ProjectorManagerDefClass(void);
	~ProjectorManagerDefClass(void);

	void											Validate_Parameters(void);

	bool											Save(ChunkSaveClass &csave);
	bool											Load(ChunkLoadClass &cload);

public:

	bool											IsEnabled;						// should this object create a projector
	bool											IsPerspective;					// is this a perspective projection
	bool											IsAdditive;						// is this an additive projection
	bool											IsAnimated;						// is this projector animated (attached to a bone that animates?)
	float											OrthoWidth;						// width of the orthographic projection
	float											OrthoHeight;					// height of the orthographic projection
	float											HorizontalFOV;					// horizontal field of view
	float											VerticalFOV;					// vertical field of view
	float											NearZ;							// near clip plane
	float											FarZ;								// far clip plane
	float											Intensity;						// intensity of the projector
	StringClass									TextureName;					// name of texture to project
	StringClass									BoneName;						// name of the bone which should control the projector

};

/*
** Use this macro to make all of the member variables in a ProjectorManagerDefClass editable.
** The first parameter to the macro is the type-name of your class (e.g. StaticAnimPhysDefClass) and
** the second parameter is the name of the member variable which is a ProjectorManagerDefClass.
*/
#define	PROJECTORMANAGERDEF_EDITABLE_PARAMS( class_name , member_name )																						\
