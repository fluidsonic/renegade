//
// Filename:     msgstatlistgroup.cpp
// Project:      
// Author:       Tom Spencer-Smith
// Date:         
// Description:  
//
//------------------------------------------------------------------------------------
#include "msgstatlistgroup.h" // I WANNA BE FIRST!

#include "mathutil.h"

//
// Class statics
//

//------------------------------------------------------------------------------------
cMsgStatListGroup::cMsgStatListGroup(void) :
	NumLists(0)
{
}

//------------------------------------------------------------------------------------
cMsgStatListGroup::~cMsgStatListGroup(void)
{
	if (PStatList != NULL) {
		delete [] PStatList;
		PStatList = NULL;
	}
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Init(int num_lists, int num_stats)
{

	NumLists = num_lists;
	PStatList = new cMsgStatList[NumLists + 1];

	for (int i = 0; i < NumLists + 1; i++) {
		PStatList[i].Init(num_stats);
	}
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Increment_Num_Msg_Sent(int list_num, int message_type, int increment)
{

	PStatList[list_num].Increment_Num_Msg_Sent(message_type, increment);
	PStatList[NumLists].Increment_Num_Msg_Sent(message_type, increment);
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Increment_Num_Byte_Sent(int list_num, int message_type, int increment)
{

	PStatList[list_num].Increment_Num_Byte_Sent(message_type, increment);
	PStatList[NumLists].Increment_Num_Byte_Sent(message_type, increment);
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Increment_Num_Msg_Recd(int list_num, int message_type, int increment)
{

	PStatList[list_num].Increment_Num_Msg_Recd(message_type, increment);
	PStatList[NumLists].Increment_Num_Msg_Recd(message_type, increment);
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Increment_Num_Byte_Recd(int list_num, int message_type, int increment)
{

	PStatList[list_num].Increment_Num_Byte_Recd(message_type, increment);
	PStatList[NumLists].Increment_Num_Byte_Recd(message_type, increment);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Get_Num_Msg_Sent(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Get_Num_Msg_Sent(message_type);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Get_Num_Byte_Sent(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Get_Num_Byte_Sent(message_type);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Get_Num_Msg_Recd(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Get_Num_Msg_Recd(message_type);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Get_Num_Byte_Recd(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Get_Num_Byte_Recd(message_type);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Compute_Avg_Num_Byte_Sent(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Compute_Avg_Num_Byte_Sent(message_type);
}

//-----------------------------------------------------------------------------
DWORD cMsgStatListGroup::Compute_Avg_Num_Byte_Recd(int list_num, int message_type) const
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return PStatList[list_num].Compute_Avg_Num_Byte_Recd(message_type);
}

//-----------------------------------------------------------------------------
cMsgStatList * cMsgStatListGroup::Get_Stat_List(int list_num)
{
	if (list_num == ALL_LISTS) {
		list_num = NumLists;
	}

	return &PStatList[list_num];
}

//-----------------------------------------------------------------------------
void cMsgStatListGroup::Set_Name(int message_type, LPCSTR name)
{
	for (int i = 0; i <= NumLists; i++) {
		PStatList[i].Set_Name(message_type, name);
	}
}

