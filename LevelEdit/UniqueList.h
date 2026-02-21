#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __UNIQUELIST_H
#define __UNIQUELIST_H

#include "Vector.H"

/////////////////////////////////////////////////////////////////////
//
//	UniqueListClass
//
template<class T>
class UniqueListClass : public DynamicVectorClass<T>
{
	public:
		UniqueListClass (void)
			: DynamicVectorClass<T> () {}
		virtual ~UniqueListClass (void) {}

		UniqueListClass<T> &			operator += (const UniqueListClass<T> &reference);
		bool								Add_Unique (T const & object);
		void								Remove (T const & object);
		bool								Is_Item_In_List (T const & object);
};


/////////////////////////////////////////////////////////////////////
//
//	Is_Item_In_List
//
template<class T>
bool
UniqueListClass<T>::Is_Item_In_List (T const & object)
{
	// Assume failure
	bool bfound = false;

	// Loop through all the objects in this list and see
	// if any of them match the provided object
	for (int index = 0; (index < Count ()) && !bfound; index ++) {
		if (object == Vector[index]) {
			bfound = true;
		}
	}

	// Return the true/false result code
	return bfound;
}


/////////////////////////////////////////////////////////////////////
//
//	Add_Unique
//
template<class T>
UniqueListClass<T> &
UniqueListClass<T>::operator += (const UniqueListClass<T> &reference)
{
	for (int index = 0; index < reference.Count (); index ++) {
		Add_Unique (reference[index]);
	}

	return *this;
}


/////////////////////////////////////////////////////////////////////
//
//	Add_Unique
//
template<class T>
bool
UniqueListClass<T>::Add_Unique (T const & object)
{
	// Assume sucess
	bool retval = true;

	// If the object isn't already in the list, then add it
	if (!Is_Item_In_List (object)) {
		retval = Add (object);
	}

	// Return the true/false result code
	return retval;
}


/////////////////////////////////////////////////////////////////////
//
//	Remove
//
template<class T>
void
UniqueListClass<T>::Remove (T const & object)
{
	// Loop through all the objects in this list and see
	// if any of them match the provided object
	bool bfound = false;
	for (int index = 0; (index < Count ()) && !bfound; index ++) {
		if (object == Vector[index]) {
			Delete (index);
			bfound = true;
		}
	}

	return ;
}



#endif //__UNIQUELIST_H
