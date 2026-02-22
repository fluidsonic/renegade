#include "global.h"
#include "progcall.h"

ProgressCallbackClass::ProgressCallbackClass(void)
{
	Reset();
}

ProgressCallbackClass::~ProgressCallbackClass(void)
{
}

void ProgressCallbackClass::Reset(void)
{
	TotalCost = 0;
	Completed = 0;
}

void ProgressCallbackClass::Add_Cost(int amount)
{
	TotalCost += amount;
}

void ProgressCallbackClass::Notify_Completed(int amount)
{
	Completed += amount;
}

float ProgressCallbackClass::Percent_Done(void)
{
	if (TotalCost <= 0) return 1.0f;
	return 100.0f * (float)Completed / (float)TotalCost;
}
