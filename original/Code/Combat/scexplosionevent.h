#ifndef __SCEXPLOSIONEVENT_H__
#define __SCEXPLOSIONEVENT_H__

#include "networkobject.h"
#include "netclassids.h"
#include "vector3.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent an explosion
//
class	cScExplosionEvent : public NetworkObjectClass
{
public:
   cScExplosionEvent(void);
	void						Init(int def_id, const Vector3 & position, int owner_id, int victim_id );
	virtual void			Delete(void)										{delete this;}

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32_t			Get_Network_Class_ID(void) const				{return NETCLASSID_SCEXPLOSIONEVENT;}

private:

	virtual void			Act(void);

	int						DefID;
	Vector3					Position;
	int						OwnerID;
	int						VictimID;

};

//-----------------------------------------------------------------------------

#endif	// __SCEXPLOSIONEVENT_H__
