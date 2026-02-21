#ifndef __SCPINGRESPONSEEVENT_H__
#define __SCPINGRESPONSEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C ping response event
//
class	cScPingResponseEvent : public cNetEvent
{
public:
   cScPingResponseEvent(void);

	void						Init(int sender_id, int ping_number);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_SCPINGRESPONSEEVENT;}

private:

	virtual void			Act(void);

	int						PingNumber;
};

//-----------------------------------------------------------------------------

#endif	// __SCPINGRESPONSEEVENT_H__
