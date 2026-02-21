#include "waypoint.h"
#include "persistfactory.h"
#include "wwphysids.h"
#include "wwhack.h"
#include "pathfindportal.h"


DECLARE_FORCE_LINK(waypoint);

//////////////////////////////////////////////////////////////////////////////
//	Persist factory
//////////////////////////////////////////////////////////////////////////////
SimplePersistFactoryClass<WaypointClass, PHYSICS_CHUNKID_WAYPOINT> _WaypointPersistFactory;


enum
{
	CHUNKID_VARIABLES			= 0x04290112,
	CHUNKID_BASE_CLASS
};

enum
{
	VARID_OLD_PTR				= 0x01,
	VARID_FLAGS,
	VARID_POSITION,
	VARID_ID,
	VARID_ACTION_ID
};


////////////////////////////////////////////////////////////////
//
//	WaypointClass
//
////////////////////////////////////////////////////////////////
WaypointClass::WaypointClass (void)
	:	m_ID (0),
		m_Position (0, 0, 0),
		m_Flags (0),
		m_ActionPortalID (-1)		
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	WaypointClass
//
////////////////////////////////////////////////////////////////
WaypointClass::WaypointClass (const WaypointClass &src)
	:	m_ID (0),
		m_Position (0, 0, 0),
		m_Flags (0),
		m_ActionPortalID (-1)
{
	(*this) = src;
	return ;
}


////////////////////////////////////////////////////////////////
//
//	WaypointClass
//
////////////////////////////////////////////////////////////////
WaypointClass::WaypointClass (const Vector3 &position)
	:	m_ID (0),
		m_Position (position),
		m_Flags (0),
		m_ActionPortalID (-1)
{
	return ;
}


////////////////////////////////////////////////////////////////
//
//	~WaypointClass
//
////////////////////////////////////////////////////////////////
WaypointClass::~WaypointClass (void)
{
	Free ();
	return ;
}


////////////////////////////////////////////////////////////////
//
//	Get_Factory
//
////////////////////////////////////////////////////////////////
const PersistFactoryClass &
WaypointClass::Get_Factory (void) const
{
	return _WaypointPersistFactory;
}


/////////////////////////////////////////////////////////////////
//
//	Save
//
/////////////////////////////////////////////////////////////////
bool
WaypointClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_VARIABLES);
				
		WaypointClass *this_ptr = this;
		WRITE_MICRO_CHUNK (csave, VARID_OLD_PTR,		this_ptr);
		WRITE_MICRO_CHUNK (csave, VARID_FLAGS,			m_Flags);		
		WRITE_MICRO_CHUNK (csave, VARID_POSITION,		m_Position);		
		WRITE_MICRO_CHUNK (csave, VARID_ID,				m_ID);
		WRITE_MICRO_CHUNK (csave, VARID_ACTION_ID,	m_ActionPortalID);

	csave.End_Chunk ();
	return true;
}


/////////////////////////////////////////////////////////////////
//
//	Load
//
/////////////////////////////////////////////////////////////////
bool
WaypointClass::Load (ChunkLoadClass &cload)
{
	while (cload.Open_Chunk ()) {		
		switch (cload.Cur_Chunk_ID ()) {
		
			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;
		}

		cload.Close_Chunk ();
	}

	return true;
}


///////////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
///////////////////////////////////////////////////////////////////////
bool
WaypointClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {

			READ_MICRO_CHUNK (cload, VARID_FLAGS,		m_Flags);
			READ_MICRO_CHUNK (cload, VARID_POSITION,	m_Position);
			READ_MICRO_CHUNK (cload, VARID_ID,			m_ID);
			READ_MICRO_CHUNK (cload, VARID_ACTION_ID,	m_ActionPortalID);		

			case VARID_OLD_PTR:
			{
				//
				//	Read the old pointer from the chunk and submit it
				// to the remapping system.
				//				
				WaypointClass *old_ptr = NULL;
				cload.Read (&old_ptr, sizeof (old_ptr));
				SaveLoadSystemClass::Register_Pointer (old_ptr, this);
			}
			break;
		}

		cload.Close_Micro_Chunk ();
	}

	return true;
}


///////////////////////////////////////////////////////////////////////
//
//	operator=
//
///////////////////////////////////////////////////////////////////////
const WaypointClass &
WaypointClass::operator= (const WaypointClass &src)
{
	//
	//	Simply copy the member data from the src class.
	// Note;  This is included for extendibility, in 
	// case this class every contains more then simple
	// variables.
	//	
	m_ID					= src.m_ID;
	m_Position			= src.m_Position;
	m_Flags				= src.m_Flags;
	m_ActionPortalID	= src.m_ActionPortalID;

	return (*this);
}


///////////////////////////////////////////////////////////////////////
//
//	Get_Action_Portal
//
///////////////////////////////////////////////////////////////////////
PathfindActionPortalClass *
WaypointClass::Get_Action_Portal (void)
{
	//
	//	Lookup the action portal
	//
	PathfindActionPortalClass *retval	= NULL;
	PathfindPortalClass *portal			= PathfindClass::Get_Instance ()->Peek_Portal (m_ActionPortalID);
	if (portal != NULL) {

		//
		//	Check to ensure this portal is an action portal
		//
		retval = portal->As_PathfindActionPortalClass ();
	}

	return retval;
}


///////////////////////////////////////////////////////////////////////
//
//	Set_Action_Portal
//
///////////////////////////////////////////////////////////////////////
void
WaypointClass::Set_Action_Portal (PathfindActionPortalClass *portal)
{
	if (portal != NULL) {
		m_ActionPortalID = portal->Get_ID ();
		Set_Flag (FLAG_REQUIRES_ACTION, true);
	} else {
		m_ActionPortalID = -1;
		Set_Flag (FLAG_REQUIRES_ACTION, false);
	}

	return ;
}
