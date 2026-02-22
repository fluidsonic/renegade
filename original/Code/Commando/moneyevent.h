#ifndef __MONEYEVENT_H__
#define __MONEYEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for money cheat.
//
class	cMoneyEvent : public cNetEvent
{
public:
   cMoneyEvent(void);

	void						Init(int amount);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_MONEYEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						Amount;
};

//-----------------------------------------------------------------------------

#endif	// __MONEYEVENT_H__
