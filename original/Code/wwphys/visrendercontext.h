#if defined(_MSC_VER)
#pragma once
#endif

#ifndef VISRENDERCONTEXT_H
#define VISRENDERCONTEXT_H

#include "rinfo.h"
#include "bittype.h"
#include "camera.h"
#include "vistable.h"
#include "vissample.h"
#include "vector.h"


const int BACKFACE_VIS_ID	= 0x00FFFBAC;						// Vis id for backface pixels


/*
** VisRenderContextClass 
** This is a class derived from RenderInfoClass which is used by the visibility
** system.  It adds features needed in building the pre-calculated visibility tables
** for a level.  The way this is typically used is you set the vis id, then render
** an object (and flush it) and then call the scan function to update the visibility 
** table.
*/
class VisRenderContextClass : public SpecialRenderInfoClass
{
public:

	VisRenderContextClass(CameraClass & cam,VisTableClass & vtab);
	~VisRenderContextClass(void);

	void						Set_Vis_ID(uint32 id);
	void						Set_Resolution(int resx,int resy);
	void						Get_Resolution(int * set_resx,int * set_resy);

	bool						Is_Vis_Quick_And_Dirty(void)						{ return VisIgnoreNonOccluders; }
	void						Set_Vis_Quick_And_Dirty(bool onoff)				{ VisIgnoreNonOccluders = onoff; }

	void						Scan_Frame_Buffer(VisSampleClass * sample);
	void						Scan_Frame_Buffer(const AABoxClass & wrld_bbox,VisSampleClass * sample);

	void						Clear_Color_Buffer(void);
	void						Clear_Z_Buffer(void);

	VisTableClass &		VisTable;

protected:

	void						Scan_Frame_Buffer(const Vector2 & min_v,const Vector2 & max_v,VisSampleClass * sample);
	void						Compute_2D_Bounds(const AABoxClass & wrld_bbox,Vector2 *	min_v,Vector2 * max_v);
	
	bool						VisIgnoreNonOccluders;

private:

	// Not implemented...
	VisRenderContextClass(const VisRenderContextClass &);
	VisRenderContextClass & operator = (const VisRenderContextClass &);

};


#endif // VISRENDERCONTEXT_H