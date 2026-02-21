//
// Filename:     msgstat.h
// Project:      
// Author:       Tom Spencer-Smith
// Date:         
// Description:  Send and receive stats for a single type of message.
//
//-----------------------------------------------------------------------------
#if defined(_MSV_VER)
#pragma once
#endif

#ifndef MSGSTAT_H
#define MSGSTAT_H

#pragma warning(disable:4514)

#include "bittype.h"

#ifndef NULL
#define NULL 0L
#endif

//-----------------------------------------------------------------------------
class cMsgStat
{
	public:
		cMsgStat(void);
		~cMsgStat(void);

		void		Increment_Num_Msg_Sent(		int increment = 1);
		void		Increment_Num_Byte_Sent(	int increment);
		void		Increment_Num_Msg_Recd(		int increment = 1);
		void		Increment_Num_Byte_Recd(	int increment);

		DWORD		Get_Num_Msg_Sent(void)	const	{return NumMsgSent;}
		DWORD		Get_Num_Byte_Sent(void)	const	{return NumByteSent;}
		DWORD		Get_Num_Msg_Recd(void)	const	{return NumMsgRecd;}
		DWORD		Get_Num_Byte_Recd(void)	const	{return NumByteRecd;}

		DWORD		Compute_Avg_Num_Byte_Sent(void) const;
		DWORD		Compute_Avg_Num_Byte_Recd(void) const;

		void		Set_Name(LPCSTR name);
		LPCSTR	Get_Name(void) const				{return Name;}

	private:
      cMsgStat(const cMsgStat& source);					// disallow
      cMsgStat& operator=(const cMsgStat& source);	// disallow

		DWORD		NumMsgSent;
		DWORD		NumByteSent;
		DWORD		NumMsgRecd;
		DWORD		NumByteRecd;

		char		Name[30];
};

//-----------------------------------------------------------------------------

#endif // MSGSTAT_H


