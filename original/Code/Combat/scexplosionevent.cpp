#include "scexplosionevent.h"

#include <stdio.h>
#include <stdlib.h>

#include "networkobjectfactory.h"
#include "wwaudio.h"
#include "matrix3d.h"
#include "translatedb.h"
#include "string_ids.h"
#include "apppackettypes.h"
#include "bitpackids.h"
#include "explosion.h"
#include "combat.h"


DECLARE_NETWORKOBJECT_FACTORY(cScExplosionEvent, NETCLASSID_SCEXPLOSIONEVENT);

//-----------------------------------------------------------------------------
cScExplosionEvent::cScExplosionEvent(void)
{
	DefID = 0;
	Position.Set(Vector3(0, 0, 0));
	OwnerID = 0;

	Set_App_Packet_Type(APPPACKETTYPE_SCEXPLOSIONEVENT);
}

//-----------------------------------------------------------------------------
void
cScExplosionEvent::Init(int def_id, const Vector3 & position, int owner_id, int victim_id )
{
	WWASSERT(CombatManager::I_Am_Server());

	DefID		= def_id;
	Position	= position;
	OwnerID 	= owner_id;
	VictimID = victim_id;

	//
	// This skips the local client
	//
	Set_Object_Dirty_Bit(BIT_CREATION, true);

//	if (CombatManager::I_Am_Client())
	{
		Act();
	}
}

//-----------------------------------------------------------------------------
void
cScExplosionEvent::Act(void)
{
//	WWASSERT(CombatManager::I_Am_Client());

	ExplosionManager::Explode( DefID, Position, OwnerID, VictimID );

	WWDEBUG_SAY(("cScExplosionEvent::Act: Def %d, at (%5.2f, %5.2f, %5.2f) Owner %d Victim %d\n", 
		DefID, Position.X, Position.Y, Position.Z, OwnerID, VictimID ));

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScExplosionEvent::Export_Creation(BitStreamClass & packet)
{
	WWASSERT(CombatManager::I_Am_Server());

	NetworkObjectClass::Export_Creation(packet);

	packet.Add(DefID);
	packet.Add(Position.X, BITPACK_WORLD_POSITION_X);
	packet.Add(Position.Y, BITPACK_WORLD_POSITION_Y);
	packet.Add(Position.Z, BITPACK_WORLD_POSITION_Z);
	packet.Add(OwnerID);

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScExplosionEvent::Import_Creation(BitStreamClass & packet)
{
	WWASSERT(CombatManager::I_Am_Only_Client());

	NetworkObjectClass::Import_Creation(packet);

	packet.Get(DefID);
	packet.Get(Position.X, BITPACK_WORLD_POSITION_X);
	packet.Get(Position.Y, BITPACK_WORLD_POSITION_Y);
	packet.Get(Position.Z, BITPACK_WORLD_POSITION_Z);
	packet.Get(OwnerID);

	Act();
}
