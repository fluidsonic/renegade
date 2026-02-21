
#ifndef BOUND_H
#define BOUND_H

template<class T> inline
T Bound(T original, T minval, T maxval)
{
	if (original < minval) return(minval);
	if (original > maxval) return(maxval);
	return(original);
};


#endif

