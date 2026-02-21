#ifndef __EVICTIONEVENT_H__
#define __EVICTIONEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
enum EvictionCodeEnum
{
	EVICTION_POOR_BANDWIDTH,
	//EVICTION_BAD_PASSWORD,
};

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent server eviction of a client
//
class	cEvictionEvent : public cNetEvent
{
public:
   cEvictionEvent(void);

	void						Init(int client_id, EvictionCodeEnum code);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_EVICTIONEVENT;}

private:

	virtual void			Act(void);

	EvictionCodeEnum		EvictionCode;
};

//-----------------------------------------------------------------------------

#endif	// __EVICTIONEVENT_H__
