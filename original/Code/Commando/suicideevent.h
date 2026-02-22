#ifndef __SUICIDEEVENT_H__
#define __SUICIDEEVENT_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for player suicide.
//
class	cSuicideEvent : public cNetEvent
{
public:
   cSuicideEvent(void);

	void						Init(void);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_SUICIDEEVENT;}

private:

	virtual void			Act(void);// const;

	int						SenderId;
};

//-----------------------------------------------------------------------------

#endif	// __SUICIDEEVENT_H__
