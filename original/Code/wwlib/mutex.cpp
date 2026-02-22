#include "global.h"
#include "mutex.h"
#include <windows.h>

// ----------------------------------------------------------------------------

MutexClass::MutexClass(const char* name)
{
		//assert(0);
}

MutexClass::~MutexClass()
{
		//assert(0);
}

bool MutexClass::Lock(int time)
{
		//assert(0);
		return true;
}

void MutexClass::Unlock()
{
		//assert(0);
}

// ----------------------------------------------------------------------------

MutexClass::LockClass::LockClass(MutexClass& mutex_,int time) : mutex(mutex_)
{
	failed=!mutex.Lock(time);
}

MutexClass::LockClass::~LockClass()
{
	if (!failed) mutex.Unlock();
}

// CriticalSectionClass is implemented in critsection.cpp.
