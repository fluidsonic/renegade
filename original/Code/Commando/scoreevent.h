#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for score cheat.
//
class	cScoreEvent : public cNetEvent
{
public:
   cScoreEvent(void);

	void						Init(int amount);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_SCOREEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						Amount;
};

//-----------------------------------------------------------------------------
