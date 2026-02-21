#include "visoptprogress.h"


/************************************************************************************
**
** VisOptProgressClass Implementation
**
************************************************************************************/

VisOptProgressClass::VisOptProgressClass(void) 
{
	Reset(0);
}

void VisOptProgressClass::Reset(int total_operation_count)
{
	TotalOps = total_operation_count;
	CompletedOps = 0;

	InitialBitCount = 0;
	FinalBitCount = 0;
	InitialSectorCount = 0;
	FinalSectorCount = 0;
	InitialObjectCount = 0;
	FinalObjectCount = 0;
	InitialDynamicCellCount = 0;
	FinalDynamicCellCount = 0;
	DynCellsRemoved = 0;
	ObjectsMerged = 0;
	SectorsMerged = 0;
}




