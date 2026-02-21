#ifndef __SVRGOODBYEEVENT_H__
#define __SVRGOODBYEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent server destruction
//
class	cSvrGoodbyeEvent : public cNetEvent
{
public:
   cSvrGoodbyeEvent(void);

	void						Init(bool flag);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_SVRGOODBYEEVENT;}

private:

	virtual void			Act(void);

	bool						IsQuickFullExitRequested;
};

//-----------------------------------------------------------------------------

#endif	// __SVRGOODBYEEVENT_H__
