
#ifndef PIVOT_H
#define PIVOT_H

#include "always.h"
#include "vector3.h"
#include "matrix3d.h"
#include "quat.h"
#include "w3d_file.h"


/*

	PivotClass

	Each node of the hierarchy tree is represented by a 
	PivotClass.
	
*/



struct PivotClass
{
public:

	PivotClass(void);
	~PivotClass(void) {}

	char					Name[W3D_NAME_LEN];
	int					Index;

	PivotClass *		Parent;

	// Base configuration of this pivot
	Matrix3D				BaseTransform;		// base-pose transform (relative to parent).

	Matrix3D				Transform;			// computed transform for this pivot
	bool					IsVisible;			// result of the visibility channel
	
	// User control.  When a pivot is 'captured' animation data is ignored and the
	// user data is used to control the pivot.
	bool					IsCaptured;
	Matrix3D				CapTransform;
	bool					WorldSpaceTranslation;

	void Capture_Update(void);

};



#endif