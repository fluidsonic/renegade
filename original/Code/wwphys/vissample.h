#pragma once

#include "global.h"

#include "visenum.h"
#include "matrix3d.h"

// Forward declarations
class ChunkSaveClass;
class ChunkLoadClass;

/*
** VisSampleClass
** This class encapsulates the calculations to set up a vis-sample and the results
** of that sample.
*/
class VisSampleClass
{
public:
	VisSampleClass(void);
	VisSampleClass(const Matrix3D & inital_view,VisDirBitsType direction_bits);
	void					Init_Error(void);

	bool					Save(ChunkSaveClass &chunk_save) const;
	bool					Load(ChunkLoadClass &chunk_load);

	bool					Sample_Rejected(void) const;
	bool					Sample_Useless(void) const;
	bool					Direction_Enabled(VisDirType direction_index) const;
	Matrix3D				Get_Camera_Transform(VisDirType direction_index) const;
	int					Sample_Status(VisDirType direction_index) const;
	float					Backface_Fraction(VisDirType direction_index) const;
	float					Get_Biggest_Fraction(void) const;
	int					Get_Biggest_Fraction_Index(void) const;
	int					Get_Cur_Direction(void) const;
	const char *		Get_Cur_Direction_Name(void) const;	

	void					Set_Cur_Direction(VisDirType direction_index);
	void					Set_Results(VisStatusType status,float fraction);

	int					Get_Bits_Changed(void)			{ return BitsChanged; }
	void					Set_Bits_Changed(int count)	{ BitsChanged = count; }

protected:

	Matrix3D				ViewTransform;			// intial view transform
	int					DirectionBits;			// which directions were enabled for this sample
	VisDirType			CurDirection;
	VisStatusType		Status[6];				// sample status for each direction
	float					BackfaceFraction[6];	// fraction of pixels that were backfacing for each direction
	int					BitsChanged;			// number of bits that changed with this sample

};
