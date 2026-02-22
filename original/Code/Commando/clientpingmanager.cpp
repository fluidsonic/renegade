#include "global.h"
#include "clientpingmanager.h"

#include <windows.h>
#include "systimer.h"

#include "gamemode.h"
#include "cspingrequestevent.h"
#include "cnetwork.h"

//
// Class statics
//
int		cClientPingManager::PingNumber						= 0;
DWORD		cClientPingManager::TimeSentMs						= 0;
DWORD		cClientPingManager::LastRoundTripPingMs			= 0;
DWORD		cClientPingManager::AvgRoundTripPingMs				= 0;
bool		cClientPingManager::IsAwaitingResponse				= false;
DWORD		cClientPingManager::RoundTripPingSamplesMs[];

//-----------------------------------------------------------------------------
void
cClientPingManager::Init
(
	void
)
{
	PingNumber				= 0;
	TimeSentMs				= 0;
	LastRoundTripPingMs	= 0;
	AvgRoundTripPingMs	= 0;
	IsAwaitingResponse	= false;

	for (int i = 0; i < MAX_SAMPLES; i++)
	{
		RoundTripPingSamplesMs[i] = 0;
	}
}

//-----------------------------------------------------------------------------
void
cClientPingManager::Think
(
	void
)
{
	if (GameModeManager::Find("Combat")->Is_Active() && cNetwork::I_Am_Only_Client())
	{
		if (!IsAwaitingResponse)
		{
			DWORD time_now_ms = static_cast<uint32_t>(TIMEGETTIME());
			if (time_now_ms - TimeSentMs >= MIN_PING_DELAY_MS)
			{
				PingNumber++;
				TimeSentMs = time_now_ms;
				IsAwaitingResponse = true;

				cCsPingRequestEvent * p_event = new cCsPingRequestEvent;
				p_event->Init(PingNumber);
			}
		}

		//
		// Propagate latency data to combat
		//
		CombatManager::Set_Last_Round_Trip_Ping_Ms(LastRoundTripPingMs);
		CombatManager::Set_Avg_Round_Trip_Ping_Ms(AvgRoundTripPingMs);
	}
}

//-----------------------------------------------------------------------------
DWORD
cClientPingManager::Get_Last_Round_Trip_Ping_Ms
(
	void
)
{
	return LastRoundTripPingMs;
}

//-----------------------------------------------------------------------------
DWORD
cClientPingManager::Get_Avg_Round_Trip_Ping_Ms
(
	void
)
{
	return AvgRoundTripPingMs;
}

//-----------------------------------------------------------------------------
void
cClientPingManager::Compute_Average_Round_Trip_Ping_Ms
(
	void
)
{
	//
	// This is the average of MAX_SAMPLES non-zero pings.
	//

	AvgRoundTripPingMs = 0;

	DWORD num_pings = 0;
	DWORD total_ping = 0;

	for (int i = 0; i < MAX_SAMPLES; i++)
	{
		if (RoundTripPingSamplesMs[i] != 0)
		{
			num_pings++;
			total_ping += RoundTripPingSamplesMs[i];
		}
	}

	if (num_pings > 0)
	{
		AvgRoundTripPingMs = (DWORD)(total_ping / (float) num_pings);
	}
}

//-----------------------------------------------------------------------------
void
cClientPingManager::Response_Received
(
	int ping_number
)
{
	if (ping_number == PingNumber)
	{
		LastRoundTripPingMs = static_cast<uint32_t>(TIMEGETTIME()) - TimeSentMs;
		RoundTripPingSamplesMs[PingNumber % MAX_SAMPLES] = LastRoundTripPingMs;
		Compute_Average_Round_Trip_Ping_Ms();
		IsAwaitingResponse = false;
	}
	else
	{
	}
}
