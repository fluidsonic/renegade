#include "global.h"
#include "lightsolvecontext.h"

LightSolveContextClass::LightSolveContextClass(void) :
	OcclusionEnabled(true),
	FilteringEnabled(true),
	Observer(NULL)
{
}

LightSolveContextClass::~LightSolveContextClass(void)
{
}

void LightSolveContextClass::Update_Observer(void) 
{ 
	if (Observer) Observer->Progress_Callback(*this); 
}
