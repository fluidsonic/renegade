#ifndef __PURCHASEREQUESTEVENT_H__
#define __PURCHASEREQUESTEVENT_H__

#include "netevent.h"
#include "netclassids.h"
#include "vendor.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for C&C mode purchase requests
//
class	cPurchaseRequestEvent : public cNetEvent
{
public:
   cPurchaseRequestEvent(void);

	void						Init(VendorClass::PURCHASE_TYPE type, int item_index, int alt_skin_index);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_PURCHASEREQUESTEVENT;}

private:

	virtual void			Act(void);

	int		SenderId;
	int		PurchaseType;
	int		ItemIndex;
	int		AltSkinIndex;
};

//-----------------------------------------------------------------------------

#endif	// __PURCHASEREQUESTEVENT_H__
