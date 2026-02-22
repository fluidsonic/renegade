#ifndef __LOADINGEVENT_H__
#define __LOADINGEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object to inform server when we are loading
//
class	cLoadingEvent : public cNetEvent
{
public:
   cLoadingEvent(void);

	void						Init(bool flag);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_LOADINGEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	bool						IsLoading;
};

//-----------------------------------------------------------------------------

#endif	// __LOADINGEVENT_H__
