//
// Filename:     msgstatlist.cpp
// Project:      
// Author:       Tom Spencer-Smith
// Date:         
// Description:  
//
//------------------------------------------------------------------------------------
#include "global.h"
#include "msgstatlist.h" // I WANNA BE FIRST!

#include "mathutil.h"

//
// Class statics
//

//------------------------------------------------------------------------------------
cMsgStatList::cMsgStatList(void) :
	NumStats(0)
{
}

//---------------- --------------------------------------------------------------------
cMsgStatList::~cMsgStatList(void)
{
	if (PStat != NULL) {
		delete [] PStat;
		PStat = NULL;
	}
}

//-----------------------------------------------------------------------------
void cMsgStatList::Init(int num_stats)
{

	NumStats = num_stats;
	PStat = new cMsgStat[NumStats + 1];
}

//-----------------------------------------------------------------------------
void cMsgStatList::Increment_Num_Msg_Sent(int message_type, int increment)
{

	PStat[message_type].Increment_Num_Msg_Sent(increment);
	PStat[NumStats].Increment_Num_Msg_Sent(increment);
}

//-----------------------------------------------------------------------------
void cMsgStatList::Increment_Num_Byte_Sent(int message_type, int increment)
{

	PStat[message_type].Increment_Num_Byte_Sent(increment);
	PStat[NumStats].Increment_Num_Byte_Sent(increment);
}

//-----------------------------------------------------------------------------
void cMsgStatList::Increment_Num_Msg_Recd(int message_type, int increment)
{

	PStat[message_type].Increment_Num_Msg_Recd(increment);
	PStat[NumStats].Increment_Num_Msg_Recd(increment);
}

//-----------------------------------------------------------------------------
void cMsgStatList::Increment_Num_Byte_Recd(int message_type, int increment)
{

	PStat[message_type].Increment_Num_Byte_Recd(increment);
	PStat[NumStats].Increment_Num_Byte_Recd(increment);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Get_Num_Msg_Sent(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Get_Num_Msg_Sent();
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Get_Num_Byte_Sent(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Get_Num_Byte_Sent();
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Get_Num_Msg_Recd(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Get_Num_Msg_Recd();
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Get_Num_Byte_Recd(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Get_Num_Byte_Recd();
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Compute_Avg_Num_Byte_Sent(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Compute_Avg_Num_Byte_Sent();
}

//-----------------------------------------------------------------------------
DWORD cMsgStatList::Compute_Avg_Num_Byte_Recd(int message_type) const
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type].Compute_Avg_Num_Byte_Recd();
}

//-----------------------------------------------------------------------------
cMsgStat & cMsgStatList::Get_Stat(int message_type)
{
	if (message_type == ALL_MESSAGES) {
		message_type = NumStats;
	}

	return PStat[message_type];
}

//-----------------------------------------------------------------------------
void cMsgStatList::Set_Name(int message_type, LPCSTR name)
{

	PStat[message_type].Set_Name(name);
}

//-----------------------------------------------------------------------------
LPCSTR cMsgStatList::Get_Name(int message_type) const
{

	return PStat[message_type].Get_Name();
}
