#ifndef VISSECTORSAMPLER_H
#define VISSECTORSAMPLER_H

#include "always.h"
#include "vector3.h"

class RenderObjClass;
class MeshBuilderClass;
class SceneEditorClass;
class VisGenProgressClass;
class Matrix3D;

/**
** VisSectorSamplerClass
** This class encapsulates the process of adaptively sampling a vis sector.  It will generate
** an edge table of all of the "external" edges of the vis-sector meshes contained in the
** given model and then adaptively sample along them.
*/
class VisSectorSamplerClass
{
public:
	VisSectorSamplerClass(SceneEditorClass * scene,VisGenProgressClass * Stats,float min_point_distance,int collision_group);
	~VisSectorSamplerClass(void);

	void							Process(RenderObjClass * model);

protected:

	void							Reset(int poly_count);
	int							Collect_Polygons(RenderObjClass * model);
	void							Sample_Edges(void);
	void							Sample_Edge(const Vector3 & p0,const Vector3 & p1);
	int							Sample_Point(const Vector3 & point);
	int							Sample_Vertical_Segment(const Vector3 & p0,const Vector3 & p1);
	int							Update_Vis(const Vector3 & point);
	bool							Check_Ceiling (const Vector3 &position, float *ceiling_dist);
	bool							Is_Object_Invalid_Roof(RenderObjClass *render_obj);
	bool							Do_View_Planes_Pass (const Matrix3D &vis_transform);

	SceneEditorClass *		Scene;
	MeshBuilderClass *		MeshBuilder;
	VisGenProgressClass *	Stats;
	float							MinSampleDistance;
	int							CollisionGroup;

	float							EdgeSkipAccum;
};


#endif //VISSECTORSAMPLER_H

