#pragma once

#include "global.h"

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for requesting an object to be destroyed. Useful when 
// someone notices 2 vehicles stuck together, etc.
//
class	cRequestKillEvent : public cNetEvent
{
public:
   cRequestKillEvent(void);

	void						Init(int object_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_REQUESTKILLEVENT;}

private:

	virtual void			Act(void);

	int						ObjectId;
};

//-----------------------------------------------------------------------------
