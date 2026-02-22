#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent gamedata updates
//
class	cGameDataUpdateEvent : public cNetEvent
{
public:
   cGameDataUpdateEvent(void);

	void						Init(int client_id = -1);
	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_GAMEDATAUPDATEEVENT;}

private:

	virtual void			Act(void);

	int						TimeRemainingSeconds;
	//bool						ServerIsGameplayPermitted;
	int						HostedGameNumber;
};

//-----------------------------------------------------------------------------
