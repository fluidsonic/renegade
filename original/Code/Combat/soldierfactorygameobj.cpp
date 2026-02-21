#include "soldierfactorygameobj.h"
#include "basecontroller.h"
#include "simpledefinitionfactory.h"
#include "persistfactory.h"
#include "definitionmgr.h"
#include "combatchunkid.h"
#include "debug.h"
#include "basecontroller.h"
#include "combat.h"

////////////////////////////////////////////////////////////////
//	Hacks
////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////
//	Editable and persist factories
////////////////////////////////////////////////////////////////
SimplePersistFactoryClass	<SoldierFactoryGameObjDef,	CHUNKID_GAME_OBJECT_DEF_SOLDIER_FACTORY>							_SoldierFactoryGameObjDefPersistFactory;
SimplePersistFactoryClass	<SoldierFactoryGameObj,		CHUNKID_GAME_OBJECT_SOLDIER_FACTORY>								_SoldierFactoryGameObjPersistFactory;
DECLARE_DEFINITION_FACTORY (SoldierFactoryGameObjDef,	CLASSID_GAME_OBJECT_DEF_SOLDIER_FACTORY, "Soldier Factory")	_SoldierFactoryGameObjDefDefFactory;

////////////////////////////////////////////////////////////////
//	Save/Load constants
////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_DEF_PARENT						=	0x02211153,
	CHUNKID_DEF_VARIABLES,

	MICROCHUNKID_DEF_UNUSED					= 1,
};

enum
{
	CHUNKID_PARENT								=	0x02211154,
	CHUNKID_VARIABLES,

	MICROCHUNKID_UNUSED						= 1,
};

////////////////////////////////////////////////////////////////
//
//	SoldierFactoryGameObjDef
//
////////////////////////////////////////////////////////////////
SoldierFactoryGameObjDef::SoldierFactoryGameObjDef (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	~SoldierFactoryGameObjDef
//
////////////////////////////////////////////////////////////////
SoldierFactoryGameObjDef::~SoldierFactoryGameObjDef (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Get_Class_ID
//
////////////////////////////////////////////////////////////////
uint32
SoldierFactoryGameObjDef::Get_Class_ID (void) const	
{ 
	return CLASSID_GAME_OBJECT_DEF_SOLDIER_FACTORY;
}

////////////////////////////////////////////////////////////////
//
//	Create
//
////////////////////////////////////////////////////////////////
PersistClass *
SoldierFactoryGameObjDef::Create (void) const 
{
	SoldierFactoryGameObj *building = new SoldierFactoryGameObj;
	building->Init (*this);

	return building;
}

////////////////////////////////////////////////////////////////
//
//	Create
//
////////////////////////////////////////////////////////////////
bool
SoldierFactoryGameObjDef::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_DEF_PARENT);		
		BuildingGameObjDef::Save (csave);
	csave.End_Chunk ();

	csave.Begin_Chunk (CHUNKID_DEF_VARIABLES);
	csave.End_Chunk ();
	return true;
}

////////////////////////////////////////////////////////////////
//
//	Load
//
////////////////////////////////////////////////////////////////
bool
SoldierFactoryGameObjDef::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ())
	{
		switch (cload.Cur_Chunk_ID ())
		{
			case CHUNKID_DEF_PARENT:
				BuildingGameObjDef::Load (cload);
				break;

			case CHUNKID_DEF_VARIABLES:
				Load_Variables (cload);
				break;
	  
			default:
				Debug_Say (("Unrecognized Com Center Def chunkID\n"));
				break;

		}
		cload.Close_Chunk ();
	}

	return true;
}

////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
////////////////////////////////////////////////////////////////
void
SoldierFactoryGameObjDef::Load_Variables (ChunkLoadClass &cload)
{
	while (cload.Open_Micro_Chunk ()) {
		
		/*switch (cload.Cur_Micro_Chunk_ID ())
		{
			default:
				Debug_Say (("Unrecognized Com Center Def Variable chunkID\n"));
				break;
		}*/

		cload.Close_Micro_Chunk();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
////////////////////////////////////////////////////////////////
const PersistFactoryClass &
SoldierFactoryGameObjDef::Get_Factory (void) const 
{ 
	return _SoldierFactoryGameObjDefPersistFactory; 
}

////////////////////////////////////////////////////////////////
//
//	SoldierFactoryGameObj
//
////////////////////////////////////////////////////////////////
SoldierFactoryGameObj::SoldierFactoryGameObj (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	~SoldierFactoryGameObj
//
////////////////////////////////////////////////////////////////
SoldierFactoryGameObj::~SoldierFactoryGameObj (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
////////////////////////////////////////////////////////////////
const PersistFactoryClass &
SoldierFactoryGameObj::Get_Factory (void) const 
{
	return _SoldierFactoryGameObjPersistFactory;
}

////////////////////////////////////////////////////////////////
//
//	Init
//
////////////////////////////////////////////////////////////////
void SoldierFactoryGameObj::Init (void)
{
	Init (Get_Definition ());
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Init
//
////////////////////////////////////////////////////////////////
void
SoldierFactoryGameObj::Init (const SoldierFactoryGameObjDef &definition)
{
	BuildingGameObj::Init (definition);	
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Get_Definition
//
////////////////////////////////////////////////////////////////
const SoldierFactoryGameObjDef &
SoldierFactoryGameObj::Get_Definition (void) const
{
	return (const SoldierFactoryGameObjDef &)BaseGameObj::Get_Definition ();
}

////////////////////////////////////////////////////////////////
//
//	Save
//
////////////////////////////////////////////////////////////////
bool
SoldierFactoryGameObj::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_PARENT);
		BuildingGameObj::Save (csave);
	csave.End_Chunk ();

	csave.Begin_Chunk (CHUNKID_VARIABLES);
	csave.End_Chunk ();
	return true;
}

////////////////////////////////////////////////////////////////
//
//	Load
//
////////////////////////////////////////////////////////////////
bool
SoldierFactoryGameObj::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {
		switch (cload.Cur_Chunk_ID ()) {

			case CHUNKID_PARENT:
				BuildingGameObj::Load (cload);
				break;
								
			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;

			default:
				Debug_Say (("Unrecognized Com Center chunkID\n"));
				break;
		}

		cload.Close_Chunk();
	}

	return true;
}

////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
////////////////////////////////////////////////////////////////
void
SoldierFactoryGameObj::Load_Variables (ChunkLoadClass &cload)
{
	while (cload.Open_Micro_Chunk ()) {
		
		/*switch (cload.Cur_Micro_Chunk_ID ())
		{
			default:
				Debug_Say (("Unrecognized Com Center Variable chunkID\n"));
				break;
		}*/

		cload.Close_Micro_Chunk();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	On_Destroyed
//
////////////////////////////////////////////////////////////////
void
SoldierFactoryGameObj::On_Destroyed (void)
{
	BuildingGameObj::On_Destroyed ();

	//
	//	Switch off the radar for each player on this team
	//
	if (BaseController != NULL && CombatManager::I_Am_Server ()) {
		BaseController->Set_Can_Generate_Soldiers (false);
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	CnC_Initialize
//
////////////////////////////////////////////////////////////////
void
SoldierFactoryGameObj::CnC_Initialize (BaseControllerClass *base)
{
	BuildingGameObj::CnC_Initialize (base);

	//
	//	Let the base know it can generate soldiers
	//
	if (BaseController != NULL) {
		BaseController->Set_Can_Generate_Soldiers (true);
	}

	return ;
}
