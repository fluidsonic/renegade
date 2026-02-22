#include "global.h"
#include "scobeliskevent.h"


#include "networkobjectfactory.h"
#include "wwaudio.h"
#include "matrix3d.h"
#include "translatedb.h"
#include "string_ids.h"
#include "apppackettypes.h"
#include "bitpackids.h"
#include "explosion.h"
#include "combat.h"

DECLARE_NETWORKOBJECT_FACTORY(cScObeliskEvent, NETCLASSID_SCOBELISKEVENT);

//-----------------------------------------------------------------------------
cScObeliskEvent::cScObeliskEvent(void)
{
	DefID = 0;
	Position.Set(Vector3(0, 0, 0));
	OwnerID = 0;

	Set_App_Packet_Type(APPPACKETTYPE_SCOBELISKEVENT);
}

//-----------------------------------------------------------------------------
void
cScObeliskEvent::Init(int def_id, const Vector3 & position, int owner_id )
{

	DefID		= def_id;
	Position	= position;
	OwnerID 	= owner_id;

	//
	// This skips the local client
	//
	Set_Object_Dirty_Bit(BIT_CREATION, true);

	if (CombatManager::I_Am_Client())
	{
		Act();
	}
}

//-----------------------------------------------------------------------------
void
cScObeliskEvent::Act(void)
{

	ExplosionManager::Explode( DefID, Position, OwnerID );

	Set_Delete_Pending();
}

//-----------------------------------------------------------------------------
void
cScObeliskEvent::Export_Creation(BitStreamClass & packet)
{

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
cScObeliskEvent::Import_Creation(BitStreamClass & packet)
{

	NetworkObjectClass::Import_Creation(packet);

	packet.Get(DefID);
	packet.Get(Position.X, BITPACK_WORLD_POSITION_X);
	packet.Get(Position.Y, BITPACK_WORLD_POSITION_Y);
	packet.Get(Position.Z, BITPACK_WORLD_POSITION_Z);
	packet.Get(OwnerID);

	Act();
}
