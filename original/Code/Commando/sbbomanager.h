#ifndef __SBBOMANAGER_H__
#define __SBBOMANAGER_H__


//-----------------------------------------------------------------------------
//
// Manager for server bandwidth budget out ("sbbo")
//
class	cSbboManager
{
public:
	static void		Reset(void);
	static void		Think(void);
	static void		Increment_Accum_Time_S_Net_Update(float time_increment_s);
	static void		Increment_Accum_Time_S_Combat_Think(float time_increment_s);
	static float	Get_Net_To_Combat_Ratio(void);
	static bool		Toggle_Is_Enabled(void);

private:

	static float	AccumTimeSNetUpdate;
	static float	AccumTimeSCombatThink;
	static float	NetToCombatRatio;
	static int		PoorRatios;
	static int		SlowSamples;
	static bool		IsEnabled;
};

//-----------------------------------------------------------------------------

#endif	// __SBBOMANAGER_H__
