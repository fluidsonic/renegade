#include "conversationremark.h"
#include "chunkio.h"

////////////////////////////////////////////////////////////////
//	Constants
////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_BASE_CLASS		= 0x01250306,
	CHUNKID_VARIABLES,
};

enum
{
	VARID_ORATORID				= 0,
	VARID_TEXTID,
	VARID_ANIMATION_NAME
};

////////////////////////////////////////////////////////////////
//
//	ConversationRemarkClass
//
////////////////////////////////////////////////////////////////
ConversationRemarkClass::ConversationRemarkClass (void)	:
	OratorID (0),
	TextID (0)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	ConversationRemarkClass
//
////////////////////////////////////////////////////////////////
ConversationRemarkClass::ConversationRemarkClass (const ConversationRemarkClass &src)	:
	OratorID (0),
	TextID (0)
{
	(*this) = src;
	return ;
}

////////////////////////////////////////////////////////////////
//
//	~ConversationRemarkClass
//
////////////////////////////////////////////////////////////////
ConversationRemarkClass::~ConversationRemarkClass (void)
{
	return ;
}

////////////////////////////////////////////////////////////////
//
//	operator=
//
////////////////////////////////////////////////////////////////
const ConversationRemarkClass &
ConversationRemarkClass::operator= (const ConversationRemarkClass &src)
{
	OratorID			= src.OratorID;
	TextID			= src.TextID;
	AnimationName	= src.AnimationName;	
	return *this;
}

////////////////////////////////////////////////////////////////
//
//	Save
//
////////////////////////////////////////////////////////////////
bool
ConversationRemarkClass::Save (ChunkSaveClass &csave)
{
	csave.Begin_Chunk (CHUNKID_VARIABLES);

		WRITE_MICRO_CHUNK_WWSTRING (csave, VARID_ANIMATION_NAME,	AnimationName);
		WRITE_MICRO_CHUNK (csave, VARID_ORATORID,						OratorID);
		WRITE_MICRO_CHUNK (csave, VARID_TEXTID,						TextID);
			
	csave.End_Chunk ();

	return true;
}

////////////////////////////////////////////////////////////////
//
//	Load
//
////////////////////////////////////////////////////////////////
bool
ConversationRemarkClass::Load (ChunkLoadClass &cload)
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
void
ConversationRemarkClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Loop through all the microchunks that define the variables
	//
	while (cload.Open_Micro_Chunk ()) {
		switch (cload.Cur_Micro_Chunk_ID ()) {

			READ_MICRO_CHUNK_WWSTRING (cload, VARID_ANIMATION_NAME,	AnimationName);
			READ_MICRO_CHUNK (cload, VARID_ORATORID,						OratorID);
			READ_MICRO_CHUNK (cload, VARID_TEXTID,							TextID);
		}

		cload.Close_Micro_Chunk ();
	}

	return ;
}
