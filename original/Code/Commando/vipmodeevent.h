#ifndef __VIPMODEEVENT_H__
#define __VIPMODEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored event object to inform server about changes in our vip mode status
//
class	cVipModeEvent : public cNetEvent
{
public:
   cVipModeEvent(void);

	void						Init(StringClass & password);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_VIPMODEEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	StringClass				Password;
};

//-----------------------------------------------------------------------------

#endif	// __VIPMODEEVENT_H__
