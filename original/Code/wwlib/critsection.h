#ifndef CRIT_SECTION
#define CRIT_SECTION

#include "always.h"
#include <windows.h>

class CriticalSectionClass
{
public:
	CriticalSectionClass();
	~CriticalSectionClass();

	class LockClass
	{
		CriticalSectionClass& crit;
	public:
		// In order to enter a critical section create a local
		// instance of LockClass with critical section as a parameter.
		LockClass(CriticalSectionClass& c);
		~LockClass();
	private:
		LockClass &operator=(const LockClass&) { return(*this); }
	};
	friend LockClass;

private:
	CRITICAL_SECTION Bar;
	bool inside;
	void Enter();
	void Exit();
};

#endif