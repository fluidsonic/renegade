#include "global.h"
#include "cover.h"
#include "chunkio.h"
#include "saveload.h"
#include "crandom.h"

/*
**
*/
DynamicVectorClass<CoverEntryClass *>		CoverManager::CoverPositions;

/*
**
*/
void	CoverManager::Init( void )
{
}

void	CoverManager::Shutdown( void )
{
	Remove_All();
}

/*
**
*/
enum	{
	CHUNKID_COVER_ENTRY					=	524001203,
};

bool	CoverManager::Save( ChunkSaveClass & csave )
{
	for ( int index = 0; index < CoverPositions.Count(); index++ ) {
		csave.Begin_Chunk( CHUNKID_COVER_ENTRY );
		CoverPositions[index]->Save( csave );
		csave.End_Chunk();
	}

	return true;
}

bool	CoverManager::Load( ChunkLoadClass & cload )
{

	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

			case CHUNKID_COVER_ENTRY:
			{
				CoverEntryClass * cover = NEW_REF( CoverEntryClass, () );
				cover->Load( cload );
				Add_Entry( cover );
				cover->Release_Ref();
				break;
			}

			default:
				Debug_Say(( "Unrecognized CombatSaveLoad chunkID\n" ));
				break;

		}
		cload.Close_Chunk();
	}
	return true;
}

/*
**
*/
void	CoverManager::Add_Entry( CoverEntryClass * entry )
{
	if ( entry != NULL ) {
		entry->Add_Ref();
		CoverPositions.Add( entry );
	}
}

void	CoverManager::Remove_Entry( CoverEntryClass * entry )
{
	if ( entry != NULL ) {
		CoverPositions.Delete( entry );
		entry->Release_Ref();
	}
}

void	CoverManager::Remove_All( void )
{
	while ( CoverPositions.Count() ) {
		CoverPositions[0]->Release_Ref();
		CoverPositions.Delete( 0 );
	}
}

/*
**
*/
CoverEntryClass * CoverManager::Request_Cover( const Vector3 & cur_pos, const Vector3 & danger, float max_dist )
{
	// Find a cover spot, not in use, within max_dist, that blocks from danger
	int best_index = -1;
	int best_dist = max_dist;
	for ( int index = 0; index < CoverPositions.Count(); index++ ) {
		CoverEntryClass * cover = CoverPositions[ index ];
		if ( cover->Get_In_Use() ) {	
			continue;		// Already in use
		}
		Vector3 range_vector = cur_pos - cover->Get_Transform().Get_Translation();
		float dist = range_vector.Length();
		if ( dist > max_dist ) {  
			continue;		// Too far away
		}

		if ( !Is_Cover_Safe( cover, danger ) ) {
			continue;		// Does not provide cover
		}

		if ( best_dist < dist ) {
			continue;		// Already ahave a better one
		}

		if ( best_index != -1 ) {
			if ( FreeRandom.Get_Int() & 1 ) {
				continue;
			}
		}

		best_index = index;
		best_dist = dist;
	}

	if ( best_index != -1 ) {
//		Debug_Say(( "Chose cover # %d\n", best_index ));
		CoverPositions[ best_index ]->Set_In_Use( true );
		return CoverPositions[best_index];
	}

	return NULL;
}

void	CoverManager::Release_Cover( CoverEntryClass * entry )
{
	entry->Set_In_Use( false );
}

bool	CoverManager::Is_Cover_Safe( CoverEntryClass * cover, const Vector3 & danger )
{

	Vector3 danger_local;
	Matrix3D::Inverse_Transform_Vector( cover->Get_Transform(), danger, &danger_local );
	if ( danger_local.X < WWMath::Fabs(danger_local.Y) ) {
		return false;
	}
	return true;
}

/*
**
*/
enum	{
	CHUNKID_VARIABLES								= 524001323,

	MICROCHUNKID_TRANSFORM						= 1,
	MICROCHUNKID_CROUCH,
	MICROCHUNKID_IN_USE,
	MICROCHUNKID_ATTACK_POSITION,
	MICROCHUNKID_REMAP_PTR,
};

bool	CoverEntryClass::Save( ChunkSaveClass & csave )
{
	CoverEntryClass * me = this;

	csave.Begin_Chunk( CHUNKID_VARIABLES );
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_TRANSFORM,     Transform );				
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_CROUCH,        Crouch );				
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_IN_USE,        InUse );				
		for ( int i = 0; i < AttackPositionList.Count(); i++ ) {
			Vector3 pos = AttackPositionList[i];
			WRITE_MICRO_CHUNK( csave, MICROCHUNKID_ATTACK_POSITION,     pos );				
		}
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_REMAP_PTR,     me );				
	csave.End_Chunk();

	return true;
}

bool	CoverEntryClass::Load( ChunkLoadClass & cload )
{

	CoverEntryClass * old_me = NULL;

	while (cload.Open_Chunk()) {
		switch(cload.Cur_Chunk_ID()) {

			case CHUNKID_VARIABLES:
				while (cload.Open_Micro_Chunk()) {
					switch(cload.Cur_Micro_Chunk_ID()) {

						READ_MICRO_CHUNK( cload, MICROCHUNKID_TRANSFORM,     Transform );				
						READ_MICRO_CHUNK( cload, MICROCHUNKID_CROUCH,        Crouch );				
						READ_MICRO_CHUNK( cload, MICROCHUNKID_IN_USE,        InUse );				

						case MICROCHUNKID_ATTACK_POSITION:
						{
							Vector3 pos;
							cload.Read(&pos,sizeof(pos));
							AttackPositionList.Add( pos );
							break;
						}

						READ_MICRO_CHUNK( cload, MICROCHUNKID_REMAP_PTR,     old_me );				

						default:
							Debug_Say(( "Unrecognized CoverEntry Variable chunkID %d\n", cload.Cur_Micro_Chunk_ID() ));
							break;

					}
					cload.Close_Micro_Chunk();
				}
				break;

			default:
				Debug_Say(( "Unrecognized CoverEntry chunkID\n" ));
				break;

		}
		cload.Close_Chunk();
	}

	// publish my remap pair
	if (old_me != NULL) {
		SaveLoadSystemClass::Register_Pointer( old_me, this );
	}

	return true;
}

Vector3 CoverEntryClass::Get_Attack_Position( Vector3 & enemy_pos )	
{ 
	// Find a cover position that will allow attack of enemy_pos (not yet)

	// default to cover spot
	Vector3	pos = Transform.Get_Translation();

	if ( AttackPositionList.Count() ) {
		int index = FreeRandom.Get_Int( AttackPositionList.Count() );
		pos = AttackPositionList[index];
	}

	return pos;
}
