#ifndef UMBRASUPPORT_H
#define UMBRASUPPORT_H

#define UMBRASUPPORT		0

#include "always.h"
#include "physlist.h"

#if (UMBRASUPPORT)

class MeshClass;
class CameraClass;
class Matrix3D;
class AABoxClass;

namespace Umbra
{
	class Cell;
	class Camera;
	class Model;
}

/**
** UmbraSupport
** This is a collection of support functions for Umbra, Hybrid's occlusion
** culling library.
*/
class UmbraSupport
{
public:

	static void						Init(void);
	static void						Shutdown(void);

	static void						Enable_Umbra(bool onoff);
	static bool						Is_Umbra_Enabled(void);
	static float					Get_Umbra_Memory_Consumption(void);

	static Umbra::Cell *			Peek_Umbra_Cell(void);
	static Umbra::Camera *		Peek_Umbra_Camera(void);
	static Umbra::Model *		Peek_Dummy_Sphere(void);
	
	static Umbra::Model *		Create_Box_Model(const AABoxClass & objbox);
	static Umbra::Model *		Create_Mesh_Model(MeshClass & mesh);

	static void						Install_Umbra_Object(PhysClass * obj);
	static void						Remove_Umbra_Object(PhysClass * obj);

	static void						Collect_Visible_Objects(const CameraClass & camera,RefPhysListClass & visible_obj_list);
	static void						Update_Umbra_Object(PhysClass * obj);

protected:

	bool								UmbraEnabled;

};

#endif //UMBRASUPPORT

#endif //UMBRASUPPORT_H
