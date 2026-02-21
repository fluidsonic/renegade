#include "critsection.h"

CriticalSectionClass::CriticalSectionClass():
inside(false)
{
#ifndef _UNIX
	InitializeCriticalSection(&Bar);
#endif
}

CriticalSectionClass::~CriticalSectionClass()
{
#ifndef _UNIX
	DeleteCriticalSection(&Bar);
#endif
}

void CriticalSectionClass::Enter()
{
#ifndef _UNIX	
	EnterCriticalSection(&Bar);
	inside=true;
#endif
}

void CriticalSectionClass::Exit()
{
#ifndef _UNIX	
	inside=false;
	LeaveCriticalSection(&Bar);	
#endif
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