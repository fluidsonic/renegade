#ifndef __PLAYERKILL_H__
#define __PLAYERKILL_H__

#include "netevent.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent player kill notification
//
class	cPlayerKill : public cNetEvent
{
public:
   cPlayerKill(void);
   //~cPlayerKill(void) {}

	void						Init(int killer_id, int victim_id);

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_PLAYERKILL;}
	//virtual void			Delete (void)										{delete this;}

private:

	virtual void			Act(void);

	int						KillerId;
	int						VictimId;
};

//-----------------------------------------------------------------------------

#endif	// __PLAYERKILL_H__
