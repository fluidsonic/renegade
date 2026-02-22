#include "global.h"
#include "gameobjobserver.h"
#include "simplevec.h"
#include "chunkio.h"
SimpleDynVecClass<GameObjObserverClass *>	  	PendingDeleteList;

int	GameObjObserverManager::NextID	= 8000000;

void	GameObjObserverManager::Delete_Register( GameObjObserverClass * observer )
{
	PendingDeleteList.Add( observer );
}

void	GameObjObserverManager::Delete_Pending( void )
{
	while ( PendingDeleteList.Count() ) {
		delete PendingDeleteList[0];
		PendingDeleteList.Delete( 0 );
	}
}

/*
**
*/
enum	{
	CHUNKID_VARIABLES						=	 918001455,

	MICROCHUNKID_NEXT_ID					=	1,
};

bool	GameObjObserverManager::Save( ChunkSaveClass & csave )
{
	csave.Begin_Chunk( CHUNKID_VARIABLES );
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_NEXT_ID, NextID );
	csave.End_Chunk();

	return true;
}

bool	GameObjObserverManager::Load( ChunkLoadClass & cload )
{
	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

			case CHUNKID_VARIABLES:
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {
						READ_MICRO_CHUNK( cload, MICROCHUNKID_NEXT_ID, NextID );
						default:
							Debug_Say(("Unhandled Variable Chunk:%d File:%s Line:%d\r\n",cload.Cur_Chunk_ID(),__FILE__,__LINE__));
							break;
					}
					cload.Close_Micro_Chunk();
				}
				break;

			default:
				Debug_Say(("Unhandled Chunk:%d File:%s Line:%d\r\n",cload.Cur_Chunk_ID(),__FILE__,__LINE__));
				break;

		}
		cload.Close_Chunk();
	}

	return true;
}
