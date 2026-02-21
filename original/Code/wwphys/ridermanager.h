#ifndef RIDERMANAGER_H
#define RIDERMANAGER_H

#include "always.h"
#include "physlist.h"

class Matrix3D;
class RenderObjClass;

/**
** RiderManagerClass
** This object is meant to be embedded in physics objects which can have other
** physics objects ride on/in them.  It keeps track of the objects which are attached
** and provides functions for updating all of them when you move.
*/
class RiderManagerClass
{
public:

	RiderManagerClass(void);
	~RiderManagerClass(void);

	void						Link_Rider(PhysClass * obj);
	void						Unlink_Rider(PhysClass * obj);
	bool						Contains(PhysClass * obj);

	void						Move_Riders(const Matrix3D & delta,RenderObjClass * carrier_sub_obj = NULL);

	NonRefPhysListClass * Get_Rider_List(void)	{ return &RiderList; }

protected:

	NonRefPhysListClass	RiderList;

};


#endif

