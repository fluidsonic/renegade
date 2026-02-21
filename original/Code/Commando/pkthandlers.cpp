#include "cnetwork.h"

#include "debug.h"
#include "networkobjectmgr.h"
#include "networkobjectfactory.h"
#include "networkobjectfactorymgr.h"
#include "playermanager.h"
#include "apppacketstats.h"
#include "specialbuilds.h"


extern char * Addr_As_String(sockaddr_in *addr);
extern bool g_is_loading;

//-----------------------------------------------------------------------------
NetworkObjectClass *
Create_Network_Object (cPacket &packet, int class_id, int network_obj_id)
{

	//
	//	Lookup the factory for this type of network object
	//
	NetworkObjectFactoryClass *factory = NetworkObjectFactoryMgrClass::Find_Factory (class_id);

	//
	//	Create the new object
	//
	NetworkObjectClass *new_object = factory->Create (packet);

	new_object->Set_Network_ID(network_obj_id);//TSS2001b

	return new_object;
}

//-----------------------------------------------------------------------------
void cNetwork::Server_Packet_Handler(cPacket & packet, int rhost_id)
{
#ifndef BETACLIENT


	/*TSS082801
	// TSS - hack
	if (g_is_loading) {
		Debug_Say(("Server flushing packet during loading\n"));
		packet.Flush();
		return;
	}
	*/

	//
	// TSS112401 XXX
	//
	if (PServerConnection->Get_Remote_Host(rhost_id) == NULL) {
		//
		// We were getting this crash:
		// pkthandlers.cpp (95) - received BIT_CREATION for existing object of type
		// APPPACKETTYPE_CSDAMAGEEVENT following messages "Insufficient bandwidth to send all guaranteed packets".
		//
		// The only scenario I can envisage is that the server destroys a client, but
		// some damage packets are on the wire. Then a new client joins, gets the same client_id,
		// and sends some damage packets with the same id's.
		//

		packet.Flush();
		return;
	}

	int network_obj_id		= packet.Get (network_obj_id);
	BYTE dirty_bits			= packet.Get (dirty_bits);
	bool is_delete_pending	= packet.Get (is_delete_pending);

	//
	//	Lookup the object this data belongs to
	//
	NetworkObjectClass *object = NetworkObjectMgrClass::Find_Object (network_obj_id);

	//
	//	Do we need to create this object?
	//
	if ((dirty_bits & NetworkObjectClass::BIT_CREATION) == NetworkObjectClass::BIT_CREATION) {

		int net_classid = packet.Get (net_classid);

		//WWASSERT (object == NULL);
		if (object != NULL)
		{

//#pragma message("(TSS) APPPACKETTYPE_CSDAMAGEEVENT workaround for unknown crash bug.\n")

			//
			//
			//
			//
			//
			//
			//
			//

			//DIE;

			//
			// So far we've only seen the problem manifest for APPPACKETTYPE_CSDAMAGEEVENT.
			// It should be safe to kill the existing conflicting object of this type.
			//
			if (object->Get_App_Packet_Type() == APPPACKETTYPE_CSDAMAGEEVENT) {
				object->Set_Delete_Pending();
			}

			//
			// Flush this packet and bail.
			//
			packet.Flush();
			return;
		}

		//
		//	Create the network object
		//
		object = Create_Network_Object (packet, net_classid, network_obj_id);
		object->Import_Creation (packet);

		/*
		//XXX
		*/
	}

	//
	// TSS092301
	//
	if (PServerConnection->Get_Remote_Host(rhost_id) == NULL) {
		//
		// This was probably a quit packet. Bail !
		//
		packet.Flush();
		return;
	}

	if (object != NULL) {

		//
		//	Do we need to modify this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_RARE) == NetworkObjectClass::BIT_RARE) {
			object->Import_Rare (packet);
		}

		//
		//	Do we need to modify this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_OCCASIONAL) == NetworkObjectClass::BIT_OCCASIONAL) {
			object->Import_Occasional (packet);
		}

		//
		//	Do we need to update this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_FREQUENT) == NetworkObjectClass::BIT_FREQUENT) {
			object->Import_Frequent (packet);
			//object->Increment_Import_State_Count ();
		}

		object->Increment_Import_State_Count();

		//
		//	Do we need to delete this object?
		//
		if (is_delete_pending) {

			//
			//	Delete the object
			//
			if (object != NULL) {
				object->Set_Delete_Pending ();
			}
		}
	} else {
		packet.Flush();
		//Debug_Network_Basic(("Server received update for non-existent object %d.\n",
		//	network_obj_id));
	}

	//
	//	Did we read all the data contained in the packet?
	//
	//assert(packet.Is_Flushed());
	if (!packet.Is_Flushed())
	{
	}

#endif // not BETACLIENT
}

//-----------------------------------------------------------------------------
void cNetwork::Client_Packet_Handler(cPacket & packet)
{
#ifndef FREEDEDICATEDSERVER


	// TSS - hack
	if (g_is_loading) {
		Debug_Say(("Client flushing packet during loading\n"));
		packet.Flush();
		return;
	}

	//
	// This is presently needed for team object creation which isn't filtered out on send
	//
	if (cNetwork::I_Am_Server()) {
		packet.Flush ();
		return;
	}

	int network_obj_id		= packet.Get (network_obj_id);
	BYTE dirty_bits			= packet.Get (dirty_bits);
	bool is_delete_pending	= packet.Get (is_delete_pending);
	//BYTE app_packet_type		= packet.Get (app_packet_type);

	//
	//	Lookup the object this data belongs to
	//
	NetworkObjectClass *object = NetworkObjectMgrClass::Find_Object (network_obj_id);

	//
	//	Do we need to create this object?
	//
	if ((dirty_bits & NetworkObjectClass::BIT_CREATION) == NetworkObjectClass::BIT_CREATION) {

		//WWASSERT (object == NULL);
		if (object != NULL)
		{
		}

		//
		//	Create the network object
		//
		int net_classid = packet.Get (net_classid);
		if (object == NULL) {
			object = Create_Network_Object (packet, net_classid, network_obj_id);
		}
		object->Import_Creation (packet);

		//
		//	HACK - HACK
		//
		if (net_classid == NETCLASSID_GAMEOBJ) {
			BaseGameObj *game_obj			= (BaseGameObj *)object;
			SmartGameObj *smart_game_obj	= game_obj->As_SmartGameObj ();
			if (smart_game_obj != NULL) {
				int control_owner = smart_game_obj->Get_Control_Owner ();
				smart_game_obj->Set_Player_Data (cPlayerManager::Find_Player (control_owner));
			}
		}

		/*
		//XXX
		*/
	}

	if (object != NULL) {

		//
		//	Do we need to delete this object?
		//
		if (is_delete_pending && object != NULL) {
			object->Set_Delete_Pending ();
		}

		//
		//	Do we need to modify this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_RARE) == NetworkObjectClass::BIT_RARE) {
			object->Import_Rare (packet);
		}

		//
		//	Do we need to modify this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_OCCASIONAL) == NetworkObjectClass::BIT_OCCASIONAL) {
			object->Import_Occasional (packet);
		}

		//
		//	Do we need to update this object?
		//
		if ((dirty_bits & NetworkObjectClass::BIT_FREQUENT) == NetworkObjectClass::BIT_FREQUENT) {
			object->Import_Frequent (packet);
			//object->Increment_Import_State_Count ();
		}

		object->Increment_Import_State_Count();
		object->Set_Last_Clientside_Update_Time(TIMEGETTIME());

		/*moving up
		//
		//	Do we need to delete this object?
		//
		if (is_delete_pending) {

			//
			//	Delete the object
			//
			if (object != NULL) {
				object->Set_Delete_Pending ();
			}
		}
		*/
	} else {
		packet.Flush();
		//Debug_Network_Basic(("Client %d received update for non-existent object %d.\n",
		//	Get_My_Id(), network_obj_id));
	}

	//
	//	Did we read all the data contained in the packet?
	//
	//assert(packet.Is_Flushed());
	if (!packet.Is_Flushed())
	{
		//DIE;
	}

#endif // !FREEDEDICATEDSERVER
}







	//BYTE app_packet_type		= packet.Get (app_packet_type);