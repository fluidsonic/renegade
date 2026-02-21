#ifndef __CSDAMAGEEVENT_H__
#define __CSDAMAGEEVENT_H__

#include "networkobject.h"
#include "netclassids.h"

//-----------------------------------------------------------------------------
//
// A C->S reliably mirrored event for client authoritative damage
//
class	cCsDamageEvent : public NetworkObjectClass
{
public:
   cCsDamageEvent(void);

	void						Init( int damager_go_id, int damagee_go_id, float damage, int warhead );

	virtual void			Export_Creation(BitStreamClass &packet);
	virtual void			Import_Creation(BitStreamClass &packet);

	virtual uint32			Get_Network_Class_ID(void) const				{return NETCLASSID_CSDAMAGEEVENT;}
	virtual void			Delete(void)										{delete this;}

	static void				Set_Are_Clients_Trusted(bool flag)			{AreClientsTrusted = flag;}
	static bool				Get_Are_Clients_Trusted(void)					{return AreClientsTrusted;}

private:

	virtual void			Act(void);

	int						SenderId;
	int						DamagerGOID;
	int						DamageeGOID;
	float						Damage;
	int						Warhead;

	static bool				AreClientsTrusted;
};

//-----------------------------------------------------------------------------

#endif	// __CSDAMAGEEVENT_H__
