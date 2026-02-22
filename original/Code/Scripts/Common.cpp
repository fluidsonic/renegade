#include "global.h"
#include "common.h"

/******************************************************************************
*
* NAME
*     RandomVector3
*
* DESCRIPTION
*     Generate a random vector.
*
* INPUTS
*     xRange - Maximum X extent.
*     yRange - Maximum Y extent.
*     zRange - Maximum Z extent.
*
* RESULTS
*     Vector - Randomized vector
*
******************************************************************************/

Vector3 RandomVector3(float xRange, float yRange, float zRange)
{
	float x = Commands->Get_Random(-xRange, xRange);
	float y = Commands->Get_Random(-yRange, yRange);
	float z = Commands->Get_Random(-zRange, zRange);

	return Vector3(x, y, z);
}
