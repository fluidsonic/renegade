#include "sbbomanager.h"

#include "debug.h"
#include "cnetwork.h"

//
// Class statics
//
float		cSbboManager::AccumTimeSNetUpdate		= 0;
float		cSbboManager::AccumTimeSCombatThink		= 0;
float		cSbboManager::NetToCombatRatio			= 0;
int		cSbboManager::PoorRatios					= 0;
int		cSbboManager::SlowSamples					= 0;
bool		cSbboManager::IsEnabled						= true;

//-----------------------------------------------------------------------------
void
cSbboManager::Reset
(
	void
)
{

	AccumTimeSNetUpdate		= 0;
	AccumTimeSCombatThink	= 0;
	NetToCombatRatio			= 0;
	PoorRatios					= 0;
	SlowSamples					= 0;
}

//-----------------------------------------------------------------------------
void
cSbboManager::Think
(
	void
)
{
	//
	// This function reduces server bandwidth out if the framerate is low and we are
	// spending way too much time doing network updates.
	//

	if (!IsEnabled) 
	{
		return;
	}


	float total_time = AccumTimeSNetUpdate + AccumTimeSCombatThink;

	if (AccumTimeSCombatThink > 0 && total_time > 2) 
	{
		NetToCombatRatio = AccumTimeSNetUpdate / AccumTimeSCombatThink;
		AccumTimeSNetUpdate = 0;
		AccumTimeSCombatThink = 0;

		if (NetToCombatRatio > 5) {
			PoorRatios++;
		} else {
			PoorRatios = 0;
		}

		if (cNetwork::Get_Fps() < 20) {
			SlowSamples++;
		} else {
			SlowSamples = 0;
		}

		if (SlowSamples >= 10 && PoorRatios >= 10) {

			ULONG sbbo = cNetwork::PServerConnection->Get_Bandwidth_Budget_Out();
			if (sbbo >= 64000)
			{
				sbbo *= 0.90;
				cNetwork::PServerConnection->Set_Bandwidth_Budget_Out(sbbo);
				Debug_Say(("cSbboManager::Think: reducing sbbo to %d\n", sbbo));
				SlowSamples = 0;
				PoorRatios = 0;
			}
		}
	}
}
			
//-----------------------------------------------------------------------------
void
cSbboManager::Increment_Accum_Time_S_Net_Update
(
	float time_increment_s
)
{

	AccumTimeSNetUpdate += time_increment_s;
}

//-----------------------------------------------------------------------------
void
cSbboManager::Increment_Accum_Time_S_Combat_Think
(
	float time_increment_s
)
{

	AccumTimeSCombatThink += time_increment_s;
}

//-----------------------------------------------------------------------------
float
cSbboManager::Get_Net_To_Combat_Ratio
(
	void
)
{
	
	return NetToCombatRatio;
}

//-----------------------------------------------------------------------------
bool
cSbboManager::Toggle_Is_Enabled
(
	void
)
{
	IsEnabled = !IsEnabled;

	Reset();

	return IsEnabled;
}
