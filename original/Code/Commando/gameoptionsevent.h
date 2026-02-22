#ifndef __GAMEOPTIONSEVENT_H__
#define __GAMEOPTIONSEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent S->C notification of game options
//
class	cGameOptionsEvent : public cNetEvent
{
public:
   cGameOptionsEvent(void);

	void						Init(int client_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_GAMEOPTIONSEVENT;}

private:

	virtual void			Act(void);

	int						HostedGameNumber;
};

//-----------------------------------------------------------------------------

#endif	// __GAMEOPTIONSEVENT_H__
