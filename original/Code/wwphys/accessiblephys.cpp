#include "accessiblephys.h"
#include "wwphysids.h"
#include "persistfactory.h"
#include "simpledefinitionfactory.h"

/////////////////////////////////////////////////////////////////////////
//	Persist and definition factories
/////////////////////////////////////////////////////////////////////////
SimplePersistFactoryClass	<AccessiblePhysClass,		PHYSICS_CHUNKID_ACCESSIBLEPHYS>					_AccessiblePhysClassFactory;
SimplePersistFactoryClass	<AccessiblePhysDefClass,	PHYSICS_CHUNKID_ACCESSIBLEPHYSDEF>				_AccessiblePhysDefFactory;
DECLARE_DEFINITION_FACTORY	(AccessiblePhysDefClass,	CLASSID_ACCESSIBLEPHYSDEF, "AccessiblePhys")	_AccessiblePhysDefDefFactory;

/////////////////////////////////////////////////////////////////////////
//	Save/load constants
/////////////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_PARENT				= 0x10311235,
	CHUNKID_VARIABLES,
	
	VARID_LOCKCODE				= 1,
};										

enum
{
	CHUNKID_DEF_PARENT		= 0x10311249,
	CHUNKID_DEF_VARIABLES,
	
	VARID_DEF_LOCKCODE		= 1,
};										

/////////////////////////////////////////////////////////////////////////
//
//	AccessiblePhysClass
//
/////////////////////////////////////////////////////////////////////////
AccessiblePhysClass::AccessiblePhysClass (void) :
	LockCode (0)
{
	return ;
}

/////////////////////////////////////////////////////////////////////////
//
//	~AccessiblePhysClass
//
/////////////////////////////////////////////////////////////////////////
AccessiblePhysClass::~AccessiblePhysClass (void)
{
	return ;
}

/////////////////////////////////////////////////////////////////////////
//
//	Init
//
/////////////////////////////////////////////////////////////////////////
void
AccessiblePhysClass::Init (const AccessiblePhysDefClass &definition)
{
	LockCode = definition.LockCode;
	StaticAnimPhysClass::Init ((const StaticAnimPhysDefClass &)definition);
	return ;
}

/////////////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
/////////////////////////////////////////////////////////////////////////
const PersistFactoryClass &
AccessiblePhysClass::Get_Factory (void) const
{
	return _AccessiblePhysClassFactory;
}

/////////////////////////////////////////////////////////////////////////
//
//	Save
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_PARENT);
		StaticAnimPhysClass::Save (csave);
	csave.End_Chunk ();

	csave.Begin_Chunk (CHUNKID_VARIABLES);
		Save_Variables (csave);
	csave.End_Chunk ();	
	return true;
}

/////////////////////////////////////////////////////////////////////////
//
//	Save_Variables
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysClass::Save_Variables (ChunkSaveClass &csave)
{
	WRITE_MICRO_CHUNK (csave, VARID_LOCKCODE, LockCode);
	return true;
}

/////////////////////////////////////////////////////////////////////////
//
//	Load
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {
		
		switch(cload.Cur_Chunk_ID ()) 
		{
			case CHUNKID_PARENT:
				StaticAnimPhysClass::Load (cload);
				break;

			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;
				
			default:
				break;
		}
		
		cload.Close_Chunk();
	}

	return true;
}

///////////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
///////////////////////////////////////////////////////////////////////
bool
AccessiblePhysClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {
			
			READ_MICRO_CHUNK (cload, VARID_LOCKCODE, LockCode);
		}

		cload.Close_Micro_Chunk ();
	}

	return true;
}

///////////////////////////////////////////////////////////////////////
//
//	AccessiblePhysDefClass
//
///////////////////////////////////////////////////////////////////////
AccessiblePhysDefClass::AccessiblePhysDefClass (void) :
	LockCode (0)
{
	EDITABLE_PARAM (AccessiblePhysDefClass, ParameterClass::TYPE_INT, LockCode);
	return ;
}

///////////////////////////////////////////////////////////////////////
//
//	Get_Class_ID
//
///////////////////////////////////////////////////////////////////////
uint32
AccessiblePhysDefClass::Get_Class_ID (void) const	
{ 
	return CLASSID_ACCESSIBLEPHYSDEF; 
}

///////////////////////////////////////////////////////////////////////
//
//	Create
//
///////////////////////////////////////////////////////////////////////
PersistClass *
AccessiblePhysDefClass::Create (void) const
{
	AccessiblePhysClass * obj = NEW_REF(AccessiblePhysClass,());
	obj->Init(*this);
	return obj;
}

///////////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
///////////////////////////////////////////////////////////////////////
const
PersistFactoryClass &
AccessiblePhysDefClass::Get_Factory (void) const
{
	return _AccessiblePhysDefFactory;
}

///////////////////////////////////////////////////////////////////////
//
//	Is_Type
//
///////////////////////////////////////////////////////////////////////
bool
AccessiblePhysDefClass::Is_Type (const char *type_name)
{
	bool retval = false;

	if (::stricmp (type_name, AccessiblePhysDefClass::Get_Type_Name ()) == 0) {
		retval = true;
	} else {
		retval = StaticAnimPhysDefClass::Is_Type (type_name);
	}

	return retval;
}

/////////////////////////////////////////////////////////////////////////
//
//	Save
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysDefClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_DEF_PARENT);
		StaticAnimPhysDefClass::Save (csave);
	csave.End_Chunk ();

	csave.Begin_Chunk (CHUNKID_DEF_VARIABLES);
		Save_Variables (csave);
	csave.End_Chunk ();
	return true;
}

/////////////////////////////////////////////////////////////////////////
//
//	Save_Variables
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysDefClass::Save_Variables (ChunkSaveClass &csave)
{
	WRITE_MICRO_CHUNK (csave, VARID_DEF_LOCKCODE, LockCode);
	return true;
}

/////////////////////////////////////////////////////////////////////////
//
//	Load
//
/////////////////////////////////////////////////////////////////////////
bool
AccessiblePhysDefClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {
		
		switch(cload.Cur_Chunk_ID ()) 
		{
			case CHUNKID_DEF_PARENT:
				StaticAnimPhysDefClass::Load (cload);
				break;

			case CHUNKID_DEF_VARIABLES:
				Load_Variables (cload);
				break;
				
			default:
				break;
		}
		
		cload.Close_Chunk();
	}

	return true;
}

///////////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
///////////////////////////////////////////////////////////////////////
bool
AccessiblePhysDefClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {
			
			READ_MICRO_CHUNK (cload, VARID_DEF_LOCKCODE, LockCode);
		}

		cload.Close_Micro_Chunk ();
	}

	return true;
}

