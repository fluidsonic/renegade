#pragma once

#include "global.h"

#include "networkobject.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent network events
//
class	cNetEvent : public NetworkObjectClass
{
public:

	void						Init(void);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				= 0;
	virtual void			Delete(void)										{delete this;}

	//void						Send_Immediately(void);//TSS2001e

private:

	virtual void			Act(void) = 0;
};

//-----------------------------------------------------------------------------
