#ifndef __CSPINGREQUESTEVENT_H__
#define __CSPINGREQUESTEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S ping request event
//
class	cCsPingRequestEvent : public cNetEvent
{
public:
   cCsPingRequestEvent(void);

	void						Init(int ping_number);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_CSPINGREQUESTEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						PingNumber;
};

//-----------------------------------------------------------------------------

#endif	// __CSPINGREQUESTEVENT_H__
