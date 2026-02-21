//
// Filename:     msgstatlist.h
// Project:      
// Author:       Tom Spencer-Smith
// Date:         
// Description:  Collection of message stats for all message types
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef MSGSTATLIST_H
#define MSGSTATLIST_H

#include "msgstat.h"

#ifndef NULL
#define NULL 0L
#endif

//-----------------------------------------------------------------------------
class cMsgStatList
{
	public:
		cMsgStatList(void);
		~cMsgStatList(void);

		enum		{ALL_MESSAGES = -1};

		void		Init(int num_stats);

		void		Increment_Num_Msg_Sent(		int message_type, int increment = 1);
		void		Increment_Num_Byte_Sent(	int message_type, int increment);
		void		Increment_Num_Msg_Recd(		int message_type, int increment = 1);
		void		Increment_Num_Byte_Recd(	int message_type, int increment);

		DWORD		Get_Num_Msg_Sent(		int message_type) const;
		DWORD		Get_Num_Byte_Sent(	int message_type) const;
		DWORD		Get_Num_Msg_Recd(		int message_type) const;
		DWORD		Get_Num_Byte_Recd(	int message_type) const;

		DWORD		Compute_Avg_Num_Byte_Sent(	int message_type) const;
		DWORD		Compute_Avg_Num_Byte_Recd(	int message_type) const;

		cMsgStat & Get_Stat(int message_type);
		int		Get_Num_Stats(void) const		{return NumStats;}

		void		Set_Name(int message_type, LPCSTR name);
		LPCSTR	Get_Name(int message_type) const;

	private:
      cMsgStatList(const cMsgStatList& source);					// disallow
      cMsgStatList& operator=(const cMsgStatList& source);	// disallow

		cMsgStat *	PStat;
		int			NumStats;
};

//-----------------------------------------------------------------------------

#endif // MSGSTATLIST_H
