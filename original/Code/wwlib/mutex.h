#ifndef MUTEX_H
#define MUTEX_H

#if defined(_MSC_VER)
#pragma once
#endif

#include "always.h"
#include "thread.h"


// Always use mutex or critical section when accessing the same data from multiple threads!

// ----------------------------------------------------------------------------
//
// Mutex class is an expensive way of synchronization! Use critical sections
// (below) for all synchronization. Use mutexes for inter-process locking.
//
// ----------------------------------------------------------------------------

class MutexClass
{
	void* handle;
	unsigned locked;

	// Lock and unlock are private so that you can't use them directly. Use LockClass as a sentry instead!
	// Lock returns true if lock was succesful, false otherwise
	bool Lock(int time);
	void Unlock();

public:
	// Name can (and usually should) be NULL. Use name only if you wish to create a globally unique mutex
	MutexClass(const char* name = NULL);
	~MutexClass();

	enum {
		WAIT_INFINITE=-1
	};

	class LockClass
	{
		MutexClass& mutex;
		bool failed;
	public:

		// In order to lock a mutex create a local instance of LockClass with mutex as a parameter.
		// Time is in milliseconds, INFINITE means infinite wait.
		LockClass(MutexClass& m, int time=MutexClass::WAIT_INFINITE);
		~LockClass();

		// Returns true if the lock failed
		bool Failed() { return failed; }
	private:
		LockClass &operator=(const LockClass&) { return(*this); }
	};
	friend class LockClass;
};

// ----------------------------------------------------------------------------
//
// Critical sections are faster than mutex classes and they should be used
// for all synchronization.
//
// ----------------------------------------------------------------------------

class CriticalSectionClass
{
	void* handle;
	unsigned locked;

	// Lock and unlock are private so that you can't use them directly. Use LockClass as a sentry instead!
	void Lock();
	void Unlock();

public:
	// Name can (and usually should) be NULL. Use name only if you wish to create a globally unique mutex
	CriticalSectionClass();
	~CriticalSectionClass();

	class LockClass
	{
		CriticalSectionClass& CriticalSection;
	public:
		// In order to lock a mutex create a local instance of LockClass with mutex as a parameter.
		LockClass(CriticalSectionClass& c);
		~LockClass();
	private:
		LockClass &operator=(const LockClass&) { return(*this); }
	};
	friend class LockClass;
};

// ----------------------------------------------------------------------------
//
// Fast critical section is really fast version of CriticalSection. The downside
// of it is that it can't be locked multiple times from the same thread.
//
// ----------------------------------------------------------------------------

class FastCriticalSectionClass
{
	volatile unsigned Flag;

	void Thread_Safe_Set_Flag()
	{
#if defined(_MSC_VER)
		volatile unsigned& nFlag=Flag;

		#define ts_lock _emit 0xF0
		assert(((unsigned)&nFlag % 4) == 0);

		__asm mov ebx, [nFlag]
		__asm ts_lock
		__asm bts dword ptr [ebx], 0
		__asm jc The_Bit_Was_Previously_Set_So_Try_Again
		return;

		The_Bit_Was_Previously_Set_So_Try_Again:
		ThreadClass::Switch_Thread();
		__asm mov ebx, [nFlag]
		__asm ts_lock
		__asm bts dword ptr [ebx], 0
		__asm jc  The_Bit_Was_Previously_Set_So_Try_Again
#else
		// Portable atomic test-and-set using GCC/Clang builtins
		// Spin until we successfully acquire (bit 0 was 0, now set to 1)
		while (__atomic_test_and_set(&Flag, __ATOMIC_ACQUIRE)) {
			ThreadClass::Switch_Thread();
		}
#endif
	}

	WWINLINE void Thread_Safe_Clear_Flag()
	{
		Flag = 0;
	}

public:
	// Name can (and usually should) be NULL. Use name only if you wish to create a globally unique mutex
	FastCriticalSectionClass() : Flag(0) {}

	class LockClass
	{
		FastCriticalSectionClass& CriticalSection;
	public:
		LockClass(FastCriticalSectionClass& critical_section) : CriticalSection(critical_section)
		{
			CriticalSection.Thread_Safe_Set_Flag();
		}

		~LockClass()
		{
			CriticalSection.Thread_Safe_Clear_Flag();
		}
	private:
		LockClass &operator=(const LockClass&) { return(*this); }
	};

	friend class LockClass;
};



#endif
