#include "physcontrol.h"
#include "chunkio.h"
#include "wwdebug.h"

enum	
{
	PHYSCONTROLLER_CHUNK_VARIABLES				= 0x00000100,
	PHYSCONTROLLER_VARIABLE_MOVEVECTOR			= 0x00,
	PHYSCONTROLLER_VARIABLE_TURNLEFT,
};

bool PhysControllerClass::Save(ChunkSaveClass & csave)
{
	csave.Begin_Chunk(PHYSCONTROLLER_CHUNK_VARIABLES);
	WRITE_MICRO_CHUNK(csave,PHYSCONTROLLER_VARIABLE_MOVEVECTOR,MoveVector);
	WRITE_MICRO_CHUNK(csave,PHYSCONTROLLER_VARIABLE_TURNLEFT,TurnLeft);
	csave.End_Chunk();

	WWASSERT(WWMath::Is_Valid_Float(MoveVector.X));
	WWASSERT(WWMath::Is_Valid_Float(MoveVector.Y));
	WWASSERT(WWMath::Is_Valid_Float(MoveVector.Z));
	WWASSERT(WWMath::Is_Valid_Float(TurnLeft));
	
	return true;
}


bool PhysControllerClass::Load(ChunkLoadClass & cload)
{
	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) 
		{
			case PHYSCONTROLLER_CHUNK_VARIABLES:
			
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {
						READ_MICRO_CHUNK(cload,PHYSCONTROLLER_VARIABLE_MOVEVECTOR,MoveVector);
						READ_MICRO_CHUNK(cload,PHYSCONTROLLER_VARIABLE_TURNLEFT,TurnLeft);
					}
					cload.Close_Micro_Chunk();	
				}
				break;
			default:
				WWDEBUG_SAY(("Unhandled Chunk: 0x%X File: %s Line: %d\r\n",cload.Cur_Chunk_ID(),__FILE__,__LINE__));
				break;
		}
		cload.Close_Chunk();
	}

	WWASSERT(WWMath::Is_Valid_Float(MoveVector.X));
	WWASSERT(WWMath::Is_Valid_Float(MoveVector.Y));
	WWASSERT(WWMath::Is_Valid_Float(MoveVector.Z));
	WWASSERT(WWMath::Is_Valid_Float(TurnLeft));

	return true;
}

