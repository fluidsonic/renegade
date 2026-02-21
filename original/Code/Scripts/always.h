
#ifndef ALWAYS_H
#define ALWAYS_H

// Disable warning about exception handling not being enabled. It's used as part of STL - in a part of STL we don't use.
#pragma warning(disable : 4530)

/*
** Define for debug memory allocation to include __FILE__ and __LINE__ for every memory allocation.
** This helps find leaks.
*/
//#define STEVES_NEW_CATCHER
#ifdef _DEBUG
#endif	//_DEBUG


// Jani: Intel's C++ compiler issues too many warnings in WW libraries when using warning level 4

// Jani: MSVC doesn't necessarily inline code with inline keyword. Using __forceinline results better inlining
// and also prints out a warning if inlining wasn't possible. __forceinline is MSVC specific.
#define WWINLINE inline

/*
** Define the MIN and MAX macros.
** NOTE: Joe used to #include <minmax.h> in the various compiler header files.  This
** header defines 'min' and 'max' macros which conflict with the surrender code so
** I'm relpacing all occurances of 'min' and 'max with 'MIN' and 'MAX'.  For code which
** is out of our domain (e.g. Max sdk) I'm declaring template functions for 'min' and 'max'
*/
#define NOMINMAX

#ifndef MAX
#define MAX(a,b)            (((a) > (b)) ? (a) : (b))
#endif

#ifndef MIN
#define MIN(a,b)            (((a) < (b)) ? (a) : (b))
#endif

#ifdef min
#undef min
#endif

#ifdef max
#undef max
#endif

template <class T> T min(T a,T b)
{
	if (a<b) {
		return a;
	} else {
		return b;
	}
}

template <class T> T max(T a,T b)
{
	if (a>b) {
		return a;
	} else {
		return b;
	}
}


/*
**	This includes the minimum set of compiler defines and pragmas in order to bring the
**	various compilers to a common behavior such that the C&C engine will compile without
**	error or warning.
*/




#ifndef	NULL
	#define	NULL		0
#endif

/**********************************************************************
**	This macro serves as a general way to determine the number of elements
**	within an array.
*/
#ifndef ARRAY_SIZE
#define	ARRAY_SIZE(x)		int(sizeof(x)/sizeof(x[0]))
#endif

#ifndef size_of
#define size_of(typ,id) sizeof(((typ*)0)->id)
#endif


#endif