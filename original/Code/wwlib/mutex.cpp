#include "mutex.h"
#include <windows.h>

// ----------------------------------------------------------------------------

MutexClass::MutexClass(const char* name) : handle(NULL), locked(false)
{
	#ifdef _UNIX
		//assert(0);
	#else
		handle=CreateMutex(NULL,false,name);
	#endif
}

MutexClass::~MutexClass()
{
	#ifdef _UNIX
		//assert(0);
	#else
		CloseHandle(handle);
	#endif
}

bool MutexClass::Lock(int time)
{
	#ifdef _UNIX
		//assert(0);
		return true;
	#else
		int res = WaitForSingleObject(handle,time==WAIT_INFINITE ? INFINITE : time);
		if (res!=WAIT_OBJECT_0) return false;
		locked++;
		return true;
	#endif
}

void MutexClass::Unlock()
{
	#ifdef _UNIX
		//assert(0);
	#else
		locked--;
		int res=ReleaseMutex(handle);
	#endif
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

