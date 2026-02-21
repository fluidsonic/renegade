#ifndef __SCOBELISKEVENT_H__
#define __SCOBELISKEVENT_H__

#include "networkobject.h"
#include "netclassids.h"
#include "vector3.h"

//-----------------------------------------------------------------------------
//
// A S->C mirrored object to represent an explosion
//
class	cScObeliskEvent : public NetworkObjectClass
{
public:
   cScObeliskEvent(void);

	void						Init(int def_id, const Vector3 & position, int owner_id);
	virtual void			Delete(void)										{delete this;}

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);
	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_SCOBELISKEVENT;}

private:

	virtual void			Act(void);

	int						DefID;
	Vector3					Position;
	int						OwnerID;

};

//-----------------------------------------------------------------------------

#endif	// __SCOBELISKEVENT_H__
