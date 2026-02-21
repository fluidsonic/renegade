//
// Filename:     msgstat.cpp
// Project:      
// Author:       Tom Spencer-Smith
// Date:         
// Description:  
//
//------------------------------------------------------------------------------------
#include "msgstat.h" // I WANNA BE FIRST!

#include <string.h>

#include "mathutil.h"


//
// Class statics
//

//------------------------------------------------------------------------------------
cMsgStat::cMsgStat(void) :
	NumMsgSent(0),
	NumByteSent(0),
	NumMsgRecd(0),
	NumByteRecd(0)
{
	::strcpy(Name, "UNNAMED");
}

//---------------- --------------------------------------------------------------------
cMsgStat::~cMsgStat(void)
{
}

//---------------- --------------------------------------------------------------------
void cMsgStat::Increment_Num_Msg_Sent(int increment)	
{

	NumMsgSent += increment;
}

//---------------- --------------------------------------------------------------------
void cMsgStat::Increment_Num_Byte_Sent(int increment)
{
	
	NumByteSent += increment;
}

//---------------- --------------------------------------------------------------------
void cMsgStat::Increment_Num_Msg_Recd(int increment)
{
	
	NumMsgRecd += increment;
}

//---------------- --------------------------------------------------------------------
void cMsgStat::Increment_Num_Byte_Recd(int increment)
{
	
	NumByteRecd += increment;
}

//---------------- --------------------------------------------------------------------
DWORD	cMsgStat::Compute_Avg_Num_Byte_Sent(void) const
{
	DWORD avg = 0;
	if (NumMsgSent > 0) {
		avg = (DWORD) cMathUtil::Round(NumByteSent / (float) NumMsgSent);
	}

	return avg;
}

//---------------- --------------------------------------------------------------------
DWORD cMsgStat::Compute_Avg_Num_Byte_Recd(void) const
{
	DWORD avg = 0;
	if (NumMsgRecd > 0) {
		avg = (DWORD) cMathUtil::Round(NumByteRecd / (float) NumMsgRecd);
	}

	return avg;
}

//---------------- --------------------------------------------------------------------
void cMsgStat::Set_Name(LPCSTR name)
{

	::strcpy(Name, name);
}





