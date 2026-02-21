#ifndef __DONATEEVENT_H__
#define __DONATEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for money donations to teammates.
//
class	cDonateEvent : public cNetEvent
{
public:
   cDonateEvent(void);

	void						Init(int amount, int	recipient_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_DONATEEVENT;}

	static const int		Get_Minimum_Acceptable_Donation(void)		{return MinimumAcceptableDonation;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						Amount;
	int						RecipientId;
	static const int		MinimumAcceptableDonation;
};

//-----------------------------------------------------------------------------

#endif	// __DONATEEVENT_H__
