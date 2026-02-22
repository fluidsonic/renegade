#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent win notification
//
class	cWinEvent : public cNetEvent
{
public:
   cWinEvent(void);

	void						Init(int winner, int loser, bool is_cycle_over);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_WIN;}

private:

	virtual void			Act(void);

	int						HostedGameNumber;
	int						Winner;
	int						Loser;
	bool						IsMapCycleOver;
};

//-----------------------------------------------------------------------------
