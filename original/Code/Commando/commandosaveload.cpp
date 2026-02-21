#include "commandosaveload.h"
#include "chunkio.h"
#include "cnetwork.h"
#include "god.h"
#include "campaign.h"

/*
**
*/
CommandoSaveLoadClass	_CommandoSaveLoad;

enum	{
	CHUNKID_NETWORK								= 1011991043,
	CHUNKID_GOD,
	CHUNKID_CAMPAIGN,
};

/*
**
*/
bool	CommandoSaveLoadClass::Save( ChunkSaveClass &csave )
{

	csave.Begin_Chunk( CHUNKID_NETWORK );
	cNetwork::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_GOD );
	cGod::Save( csave );
	csave.End_Chunk();

	csave.Begin_Chunk( CHUNKID_CAMPAIGN );
	CampaignManager::Save( csave );
	csave.End_Chunk();

	return true;
}

bool	CommandoSaveLoadClass::Load( ChunkLoadClass &cload )
{

	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

			case CHUNKID_NETWORK:
				cNetwork::Load( cload );
				break;

			case CHUNKID_GOD:
				cGod::Load( cload );
				break;

			case CHUNKID_CAMPAIGN:
				CampaignManager::Load( cload );
				break;

			default:
				Debug_Say(( "Unrecognized Commando chunkID\n" ));
				break;

		}
		cload.Close_Chunk();
	}
	return true;
}

