#include "global.h"
#include "csdamageevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "combat.h"
#include "networkobjectmgr.h"
#include "gameobjmanager.h"
#include "apppackettypes.h"
#include "damage.h"
#include "armedgameobj.h"
DECLARE_NETWORKOBJECT_FACTORY(cCsDamageEvent, NETCLASSID_CSDAMAGEEVENT);

//
// Class statics
//
bool	cCsDamageEvent::AreClientsTrusted	= true;

//-----------------------------------------------------------------------------
cCsDamageEvent::cCsDamageEvent(void)
{
	SenderId		= 0;
	DamagerGOID	= 0;
	DamageeGOID = 0;
	Damage		= 0;
	Warhead		= 0;

	Set_App_Packet_Type(APPPACKETTYPE_CSDAMAGEEVENT);

	Set_Unreliable_Override(true);
}

//-----------------------------------------------------------------------------
void
cCsDamageEvent::Init
( 
	int	damager_go_id, 
	int	damagee_go_id, 
	float	damage, 
	int	warhead 
)
{

	SenderId			= CombatManager::Get_My_Id();
	DamagerGOID		= damager_go_id;
	DamageeGOID		= damagee_go_id;
	Damage			= damage;
	Warhead			= warhead;

	Set_Network_ID(NetworkObjectMgrClass::Get_New_Client_ID());

	Set_Object_Dirty_Bit(0, BIT_CREATION, true);
}

//-----------------------------------------------------------------------------
void
cCsDamageEvent::Act
(	
	void
)
{

	if (AreClientsTrusted)
	{
		// process the member data here. Include sanity checks.
		// Find the gameobj with the damagee id
		PhysicalGameObj * obj = GameObjManager::Find_PhysicalGameObj( DamageeGOID );
		if ( obj ) {
			// Make an offense object
			PhysicalGameObj * damager = GameObjManager::Find_PhysicalGameObj( DamagerGOID );
			if ( damager != NULL && damager->As_ArmedGameObj() != NULL ) {
				OffenseObjectClass offense( Damage, Warhead, damager->As_ArmedGameObj());

//				obj->Get_Defense_Object()->Do_Damage( offense );
				// We need to use apply damage extended in order to allow things to die.
				offense.ForceServerDamage = true;

				obj->Apply_Damage_Extended( offense );

//				Debug_Say(( "Applying Client damage of %f from %d to %d\n", Damage, DamagerGOID, DamageeGOID ));
			} else {
//				Debug_Say(( "Error: Client damage Damagee %d not found\n", DamageeGOID ));
			}
		} else {
//			Debug_Say(( "Error: Client damage Damagee %d not found\n", DamageeGOID ));
		}
	} else {
		Debug_Say(( "Error: Receiving Client damage when clients are not trusted\n" ));
	}

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsDamageEvent::Export_Creation
(
	BitStreamClass & packet
)
{

	NetworkObjectClass::Export_Creation(packet);

	packet.Add(SenderId);
	packet.Add(DamagerGOID);
	packet.Add(DamageeGOID);
	packet.Add(Damage);
	packet.Add(Warhead);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cCsDamageEvent::Import_Creation
(
	BitStreamClass & packet
)
{

	NetworkObjectClass::Import_Creation(packet);

	packet.Get(SenderId);
	packet.Get(DamagerGOID);
	packet.Get(DamageeGOID);
	packet.Get(Damage);
	packet.Get(Warhead);

	Act();
}
