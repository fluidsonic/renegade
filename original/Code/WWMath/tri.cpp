#include "global.h"
#include "tri.h"
#include "vector2.h"

static inline void find_dominant_plane(const TriClass & tri, int * axis1,int * axis2,int * axis3)
{
	/*
	** Find the largest component of the normal
	*/
	int ni = 0;
	float x = WWMath::Fabs(tri.N->X);
	float y = WWMath::Fabs(tri.N->Y);
	float z = WWMath::Fabs(tri.N->Z);
	float val = x;

	if (y > val) {
		ni = 1;
		val = y;
	}

	if (z > val) {
		ni = 2;
	}

	/*
	** return the indices of the two axes perpendicular
	*/
	switch (ni) 
	{
	case 0: 
		// Dominant is the X axis
		*axis1 = 1;
		*axis2 = 2;
		*axis3 = 0;
		break;
	case 1: 
		// Dominant is the Y axis
		*axis1 = 0;
		*axis2 = 2;
		*axis3 = 1;
		break;
	case 2: 
		// Dominant is the Z axis
		*axis1 = 0;
		*axis2 = 1;
		*axis3 = 2;
		break;
	}
}

/***********************************************************************************************
 * TriClass::Find_Dominant_Plane -- returns indices of the axes of the dominant plane          *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/24/99    GTH : Created.                                                                 *
 *=============================================================================================*/
void TriClass::Find_Dominant_Plane(int * axis1,int * axis2) const
{
	/*
	** Find the largest component of the normal
	*/
	int ni = 0;
	float x = WWMath::Fabs(N->X);
	float y = WWMath::Fabs(N->Y);
	float z = WWMath::Fabs(N->Z);
	float val = x;

	if (y > val) {
		ni = 1;
		val = y;
	}

	if (z > val) {
		ni = 2;
	}

	/*
	** return the indices of the two axes perpendicular
	*/
	switch (ni) 
	{
	case 0: 
		// Dominant is the X axis
		*axis1 = 1;
		*axis2 = 2;
		break;
	case 1: 
		// Dominant is the Y axis
		*axis1 = 0;
		*axis2 = 2;
		break;
	case 2: 
		// Dominant is the Z axis
		*axis1 = 0;
		*axis2 = 1;
		break;
	}
}

/***********************************************************************************************
 * TriClass::Contains_Point -- performs 2D point-in-triangle test.                             *
 *                                                                                             *
 * INPUT:                                                                                      *
 *                                                                                             *
 * OUTPUT:                                                                                     *
 *                                                                                             *
 * WARNINGS:                                                                                   *
 * Assumes that the point is in the plane of the triangle... use this after you've intersected *
 * a ray with the plane of the triangle.                                                       *
 *                                                                                             *
 * HISTORY:                                                                                    *
 *   3/24/99    GTH : Created.                                                                 *
 *=============================================================================================*/
bool TriClass::Contains_Point(const Vector3 & ipoint) const
{
/*
** New cross-product based point-containment
*/

/*
** "Optimized" version
*/
	int vi;
	int axis1 = 0;
	int axis2 = 0;
	int axis3 = 0;

	find_dominant_plane(*this,&axis1,&axis2,&axis3);

	int side_mask = 0;
	const int POS = 0x01;
	const int NEG = 0x02;

	/*
	** Compute the 2D cross product of edge0 with a vector to the point
	*/
	Vector2 edge;
	Vector2 dp;

	for (vi=0; vi<3; vi++) {
		
		int va=vi;
		int vb=(vi+1)%3;
		
		edge.Set((*V[vb])[axis1] - (*V[va])[axis1] , (*V[vb])[axis2] - (*V[va])[axis2]);
		dp.Set(ipoint[axis1] - (*V[va])[axis1] , ipoint[axis2] - (*V[va])[axis2]);
		float cross = edge.X * dp.Y - edge.Y * dp.X;

		if (cross > WWMATH_EPSILON) {
			side_mask |= POS;
		}
		if (cross < -WWMATH_EPSILON) {
			side_mask |= NEG;
		}
	}
	
	bool my_intersect = (side_mask != (POS | NEG));
	return my_intersect;

}
