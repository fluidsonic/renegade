#ifndef __WARPEVENT_H__
#define __WARPEVENT_H__

#include "netevent.h"
#include "netclassids.h"
#include	"widestring.h"

//-----------------------------------------------------------------------------
//
// A C->S mirrored object for requesting a warp of your soldier to join 
// another player soldier.
//
class	cWarpEvent : public cNetEvent
{
public:
   cWarpEvent(void);

	void						Init(WideStringClass & player_name);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_WARPEVENT;}

private:

	virtual void			Act(void);

	int						SenderId;
	WideStringClass		PlayerName;
};

//-----------------------------------------------------------------------------

#endif	// __WARPEVENT_H__
