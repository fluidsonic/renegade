#pragma once

#include "global.h"

//-----------------------------------------------------------------------------
class	cClientPingManager
{
public:
	static void		Init(void);
	static void		Think(void);
	static DWORD	Get_Last_Round_Trip_Ping_Ms(void);
	static DWORD	Get_Avg_Round_Trip_Ping_Ms(void);
	static void		Response_Received(int ping_number);

private:
	static void		Compute_Average_Round_Trip_Ping_Ms(void);

	enum				{MAX_SAMPLES			= 3};
	enum				{MIN_PING_DELAY_MS	= 1000};

	static int		PingNumber;
	static DWORD	TimeSentMs;
	static DWORD	LastRoundTripPingMs;
	static DWORD	AvgRoundTripPingMs;
	static bool		IsAwaitingResponse;
	static DWORD	RoundTripPingSamplesMs[MAX_SAMPLES];
};

//-----------------------------------------------------------------------------
