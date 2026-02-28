#include "global.h"
#include "basegameobj.h"
#include "combat.h"
#include "pscene.h"
#include "gameobjmanager.h"
#include "phys.h"
#include "timemgr.h"
#include "chunkio.h"
#include "assets.h"
#include "matinfo.h"
#include "definitionclassids.h"
#include "networkobjectfactory.h"
#include "definitionmgr.h"

//////////////////////////////////////////////////////////////
//	NetworkGameObjectFactoryClass
//////////////////////////////////////////////////////////////
class NetworkGameObjectFactoryClass : public NetworkObjectFactoryClass
{
public:
	virtual NetworkObjectClass *	Create (cPacket &packet) const;
	virtual void						Prep_Packet (NetworkObjectClass *object, cPacket &packet) const;
	virtual uint32_t						Get_Class_ID (void) const { return NETCLASSID_GAMEOBJ; }
};

//////////////////////////////////////////////////////////////
//	Prep_Packet
//////////////////////////////////////////////////////////////
void
NetworkGameObjectFactoryClass::Prep_Packet (NetworkObjectClass *object, cPacket &packet) const
{
	BaseGameObj *game_obj = (BaseGameObj *)(object);

	//
	//	Add the definition ID of the object to the packet
	//
	packet.Add (game_obj->Get_Definition ().Get_ID ());
	return ;
}

//////////////////////////////////////////////////////////////
//	Create
//////////////////////////////////////////////////////////////
NetworkObjectClass *
NetworkGameObjectFactoryClass::Create (cPacket &packet) const
{
	int definition_id = packet.Get (definition_id);

	//
	//	Lookup the definition for this object
	//
	DefinitionClass *definition = DefinitionMgrClass::Find_Definition (definition_id);

	if (definition == nullptr) {
		fprintf(stderr, "[DEFMGR] Find_Definition(%d) returned null\n", definition_id);
	}
	// FIXME: throw when definition is not found
	assert (definition != nullptr);

	//
	//	Create the new object
	//
	BaseGameObj *new_game_obj = (BaseGameObj *)definition->Create();

	//
	//	Convert the game object to a network object
	//
	return static_cast<NetworkObjectClass *> (new_game_obj);
}

//////////////////////////////////////////////////////////////
//	Static data
//////////////////////////////////////////////////////////////
static NetworkGameObjectFactoryClass _NetworkGameObjectFactory;

/*
** BaseGameObjDef
*/
enum	{
	CHUNKID_DEF_PARENT							= 1111991123,
};

bool	BaseGameObjDef::Save( ChunkSaveClass & csave )
{
	csave.Begin_Chunk( CHUNKID_DEF_PARENT );
		DefinitionClass::Save( csave );
	csave.End_Chunk();
	return true;
}

bool	BaseGameObjDef::Load( ChunkLoadClass &cload )
{
	cload.Open_Chunk();
	DefinitionClass::Load( cload );
	cload.Close_Chunk();
	return true;
}

/*
**
*/
BaseGameObj::BaseGameObj( void ) :
	Definition( NULL ),
	//DestroyType( DESTROY_NONE ),
	//ID( 0 ),
	IsPostThinkAllowed( false ),
	EnableCinematicFreeze( true )
{
	GameObjManager::Add( this );

	Set_Object_Dirty_Bit (NetworkObjectClass::BIT_CREATION, true);
}

/*
**
*/
BaseGameObj::~BaseGameObj( void )
{
	GameObjManager::Remove( this );
}

/*
**
*/
void	BaseGameObj::Init( const BaseGameObjDef & definition )
{
	Definition = &definition;
}

const BaseGameObjDef &	BaseGameObj::Get_Definition( void ) const 
{
	return *Definition;
};

/*
** BaseGameObj Save and Load
*/
enum	{
	CHUNKID_VARIABLES						=	910991407,

	XXX_MICROCHUNKID_DESTROY_TYPE		=	1,
	MICROCHUNKID_DEFINITION_ID,
	MICROCHUNKID_INSTANCE_ID,
	MICROCHUNKID_IS_PENDING_DELETE,
	MICROCHUNKID_ENABLE_CINEMATIC_FREEZE,
};

bool	BaseGameObj::Save( ChunkSaveClass & csave )
{
	csave.Begin_Chunk( CHUNKID_VARIABLES );
		bool is_delete_pending = Is_Delete_Pending ();
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_IS_PENDING_DELETE, is_delete_pending );
		int	definition_id = Definition->Get_ID();
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_DEFINITION_ID, definition_id );
		int id = Get_Network_ID ();
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_INSTANCE_ID, id );
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_ENABLE_CINEMATIC_FREEZE, EnableCinematicFreeze );
	csave.End_Chunk();

	return true;
}

bool	BaseGameObj::Load( ChunkLoadClass &cload )
{
	int id = 0;

	cload.Open_Chunk();
	while (cload.Open_Micro_Chunk()) {
		switch(cload.Cur_Micro_Chunk_ID()) {
			
			case MICROCHUNKID_IS_PENDING_DELETE:
			{
				bool is_delete_pending = false;
				LOAD_MICRO_CHUNK( cload, is_delete_pending );
				if (is_delete_pending) {
					Set_Delete_Pending ();
				}				
				break;
			}

			case MICROCHUNKID_INSTANCE_ID:
			{
				LOAD_MICRO_CHUNK( cload, id );
				//TSS
				//Set_Network_ID( id );
				break;
			}

			case	MICROCHUNKID_DEFINITION_ID:
				int definition_id;
				LOAD_MICRO_CHUNK( cload, definition_id );
				Definition = (const BaseGameObjDef*)DefinitionMgrClass::Find_Definition( definition_id );
				if ( Definition == NULL ) {
					Debug_Say(( "Definition %d not found\n.  Re-Export needed\n", definition_id ));
				}

				//
				// 07/30/01 attempting to load a level with temps will presently assert here.
				//
				break;

			READ_MICRO_CHUNK( cload, MICROCHUNKID_ENABLE_CINEMATIC_FREEZE, EnableCinematicFreeze );

			default:
				Debug_Say(( "Unrecognized BaseGameObj Variable chunkID\n" ));
				break;
			}
		cload.Close_Micro_Chunk();
	}
	cload.Close_Chunk();

	//
	//	Set the ID if necessary
	//

	if (id == 0) {

		id = Get_ID ();
		if (id == 0) {
			Set_Network_ID (NetworkObjectMgrClass::Get_New_Dynamic_ID ());
		} else {
		}

	} else {

		Set_Network_ID (id);
	}

	return true;
}
