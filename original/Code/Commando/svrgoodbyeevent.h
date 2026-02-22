#pragma once

#include "global.h"

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
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_SVRGOODBYEEVENT;}

private:

	virtual void			Act(void);

	bool						IsQuickFullExitRequested;
};

//-----------------------------------------------------------------------------
