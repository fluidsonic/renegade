#include "global.h"
#include "physcontrol.h"
#include "chunkio.h"

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
				break;
		}
		cload.Close_Chunk();
	}

	return true;
}
