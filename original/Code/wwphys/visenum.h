
#ifndef VISENUM_H
#define VISENUM_H

enum VisStatusType
{
	VIS_STATUS_OK = 0,
	VIS_STATUS_NOT_TAKEN,
	VIS_STATUS_ERROR,
	VIS_STATUS_BACKFACE_LEAK,
	VIS_STATUS_BACKFACE_OVERFLOW,
};

enum VisDirBitsType
{
	VIS_FORWARD_BIT	= 0x0001,		// comptue visibility forward
	VIS_LEFT_BIT		= 0x0002,		// compute visibility left
	VIS_BACKWARDS_BIT	= 0x0004,		// compute visibility behind
	VIS_RIGHT_BIT		= 0x0008,		// compute visibility to the right
	VIS_UP_BIT			= 0x0010,		// compute visibility up
	VIS_DOWN_BIT		= 0x0020,		// compute visiblity down
	VIS_FORCE_ACCEPT	= 0x0040,		// accept this sample no matter what!
	VIS_DONT_RECENTER = 0x0080,		// don't recenter the view-plane
	VIS_ALL				= VIS_FORWARD_BIT | VIS_LEFT_BIT | VIS_BACKWARDS_BIT | VIS_RIGHT_BIT | VIS_UP_BIT | VIS_DOWN_BIT,
};

enum VisDirType
{
	// direction indices, most functions use these
	VIS_FORWARD = 0,
	VIS_LEFT,
	VIS_BACKWARDS,
	VIS_RIGHT,
	VIS_UP,
	VIS_DOWN,
	VIS_DIRECTIONS
};

const float VIS_NEAR_CLIP	= 0.2f;
const float VIS_FAR_CLIP	= 300.0f;

#endif

