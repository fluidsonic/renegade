#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for player changing team
//
class	cChangeTeamEvent : public cNetEvent
{
public:
   cChangeTeamEvent(void);

	void						Init(void);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_CHANGETEAMEVENT;}

	static bool				Is_Change_Team_Possible(void);

private:

	virtual void			Act(void);

	int						SenderId;
};

//-----------------------------------------------------------------------------
