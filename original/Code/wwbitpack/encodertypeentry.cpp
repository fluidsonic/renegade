//
// Filename:     encodertypeentry.cpp
// Project:      wwbitpack.lib
// Author:       Tom Spencer-Smith
// Date:         June 2000
// Description:  
//
//-----------------------------------------------------------------------------
#include "encodertypeentry.h" // I WANNA BE FIRST!

#include <math.h>
#include <limits.h>

#include "wwdebug.h"
#include "miscutil.h"
#include "mathutil.h"

static const int MAX_BITS = 32;

//-----------------------------------------------------------------------------
cEncoderTypeEntry::cEncoderTypeEntry()
{
	Invalidate();
}

//-----------------------------------------------------------------------------
bool cEncoderTypeEntry::Is_Valid() const 
{
	return 
		((Max - Min > -MISCUTIL_EPSILON) && 
		 (Resolution > -MISCUTIL_EPSILON) && 
		 (BitPrecision >= 0));
}

//-----------------------------------------------------------------------------
void cEncoderTypeEntry::Invalidate()
{
	Min = 1;
	Max = -1;
	Resolution = -1;
	BitPrecision = 0;
}

//-----------------------------------------------------------------------------
bool cEncoderTypeEntry::Is_Value_In_Range(double value) const
{
	return (value >= Min - MISCUTIL_EPSILON && value <= Max + MISCUTIL_EPSILON);
}

//-----------------------------------------------------------------------------
void cEncoderTypeEntry::Init(double min, double max, double resolution)
{
	WWASSERT(!Is_Valid());

	WWASSERT(max - min > -MISCUTIL_EPSILON);
	WWASSERT(resolution > MISCUTIL_EPSILON);

	Min = min;
	Max = max;

	Calc_Bit_Precision(resolution);

	WWASSERT(Is_Valid());
}

//-----------------------------------------------------------------------------
void cEncoderTypeEntry::Init(int num_bits)
{
	WWASSERT(!Is_Valid());

	WWASSERT(num_bits > 0 && num_bits <= 32);

	Min = 0;
	BitPrecision = num_bits;
	Resolution = 1;

	UINT max = 0;
	for (int i = 0; i < num_bits; i++) {
		max += 1 << i;
	}

	Max = max;

	WWASSERT(Is_Valid());	
}

//-----------------------------------------------------------------------------
bool cEncoderTypeEntry::Scale(double value, ULONG & scaled_value)
{
	WWASSERT(Is_Valid());

	bool is_in_range = Is_Value_In_Range(value);

	if (!is_in_range) {
		value = Clamp(value);
	}

	scaled_value = static_cast<ULONG>
		(cMathUtil::Round((value - Min) / Resolution));

	return is_in_range;
}

//-----------------------------------------------------------------------------
double cEncoderTypeEntry::Unscale(ULONG u_value)
{
	WWASSERT(Is_Valid());

	double value = Min + u_value * Resolution;

	WWASSERT(Is_Value_In_Range(value));

	return value;
}

//-----------------------------------------------------------------------------
double cEncoderTypeEntry::Clamp(double value)
{
	WWASSERT(Is_Valid());

	double retval = value;
	
	if (retval < Min) {
		retval = Min;
	} else if (retval > Max) {
		retval = Max;
	}

	return retval;
}

//-----------------------------------------------------------------------------
void cEncoderTypeEntry::Calc_Bit_Precision(double resolution)
{
	// 
	// Calculate the minimum number of bits required to encode this type with
	// the specified resolution.
	//

	WWASSERT(Max - Min > -MISCUTIL_EPSILON);
	WWASSERT(resolution > MISCUTIL_EPSILON);

	double f_units = (double) ceil((Max - Min) / resolution - MISCUTIL_EPSILON) + 1;
	WWASSERT(f_units <= UINT_MAX + MISCUTIL_EPSILON);
	UINT units = (UINT) f_units;

	BitPrecision = 0;
	UINT max_units = 0;
	while (max_units < units) {
		max_units += 1 << BitPrecision;
		BitPrecision++;
		if (BitPrecision == 1) {
			max_units++;
		}
	}	

	WWASSERT(BitPrecision > 0 && BitPrecision <= MAX_BITS);
	WWASSERT(max_units > 0);

	Resolution = (Max - Min) / (double) (max_units - 1);

	/*TSS2001
	if (Resolution > 0) {
		WWASSERT(max_units == 
			(UINT) ceil((Max - Min) / Resolution - MISCUTIL_EPSILON) + 1);
	}
	*/
}
