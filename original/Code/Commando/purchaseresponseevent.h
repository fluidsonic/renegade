#ifndef __PURCHASERESPONSEEVENT_H__
#define __PURCHASERESPONSEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent C&C mode purchase responses
//
class	cPurchaseResponseEvent : public cNetEvent
{
public:
   cPurchaseResponseEvent(void);

	void						Init(int response_id, int client_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_PURCHASERESPONSEEVENT;}

private:

	virtual void			Act(void);

	int						PurchaserId;
	int						ResponseId;
};

//-----------------------------------------------------------------------------

#endif	// __PURCHASERESPONSEEVENT_H__
