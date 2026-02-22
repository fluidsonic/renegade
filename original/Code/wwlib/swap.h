#pragma once

#include "global.h"

/**********************************************************************
**	Swaps two objects.
*/
template<class T>
void swap(T & left, T & right)
{
	T temp;
	temp = left;
	left = right;
	right = temp;
}
