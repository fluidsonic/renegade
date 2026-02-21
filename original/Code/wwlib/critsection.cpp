#include "critsection.h"

CriticalSectionClass::CriticalSectionClass():
inside(false)
{
	InitializeCriticalSection(&Bar);
}

CriticalSectionClass::~CriticalSectionClass()
{
	DeleteCriticalSection(&Bar);
}

void CriticalSectionClass::Enter()
{
	EnterCriticalSection(&Bar);
	inside=true;
}

void CriticalSectionClass::Exit()
{
	inside=false;
	LeaveCriticalSection(&Bar);
}

CriticalSectionClass::LockClass::LockClass(CriticalSectionClass &c):
crit(c)
{
	crit.Enter();
}

CriticalSectionClass::LockClass::~LockClass()
{
	crit.Exit();
}
