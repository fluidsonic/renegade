#include "global.h"
#include "pivot.h"
#include "wwmath.h"

/*********************************************************************************************** 
 * PivotClass::PivotClass -- Constructor for PivotClass                                        * 
 *                                                                                             * 
 * INPUT:                                                                                      * 
 *                                                                                             * 
 * OUTPUT:                                                                                     * 
 *                                                                                             * 
 * WARNINGS:                                                                                   * 
 *                                                                                             * 
 * HISTORY:                                                                                    * 
 *   07/24/1997 GH  : Created.                                                                 * 
 *=============================================================================================*/
PivotClass::PivotClass(void) :
	Index(0),
	Parent(NULL),
	BaseTransform(1),
	Transform(1),
	IsVisible(true),
	IsCaptured(false),
	CapTransform(1),
	WorldSpaceTranslation(false)
{
}

void PivotClass::Capture_Update(void)
{
	if ( WorldSpaceTranslation ) {
		// The Translation of CapTransform is meant to be in world space,
		// so remove before applying orientation
		Matrix3D CapOrientation = CapTransform;
		CapOrientation.Set_Translation( Vector3( 0,0,0 ) );
		Matrix3D::Multiply(Transform,CapOrientation,&(Transform));
		// Now apply translation in world space
		Transform.Adjust_Translation( CapTransform.Get_Translation() );
	} else {
		Matrix3D::Multiply(Transform,CapTransform,&(Transform));
	}
}
