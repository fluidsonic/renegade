#ifndef __CLIENTGOODBYEEVENT_H__
#define __CLIENTGOODBYEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object to represent client destruction
//
class	cClientGoodbyeEvent : public cNetEvent
{
public:
   cClientGoodbyeEvent(void);

	void						Init(void);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_CLIENTGOODBYEEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
};

//-----------------------------------------------------------------------------

#endif	// __CLIENTGOODBYEEVENT_H__
