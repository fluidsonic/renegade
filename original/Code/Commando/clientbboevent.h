#ifndef __CLIENTBBOEVENT_H__
#define __CLIENTBBOEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for client bandwidth adjustment
//
class	cClientBboEvent : public cNetEvent
{
public:
   cClientBboEvent(void);

	void						Init(int bbo);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_CLIENTBBOEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						Bbo;
};

//-----------------------------------------------------------------------------

#endif	// __CLIENTBBOEVENT_H__
