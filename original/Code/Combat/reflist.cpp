#include "reflist.h"
#include "scriptablegameobj.h"




bool	ReferencerClass::Save( ChunkSaveClass & csave )
{
	csave.Begin_Chunk( CHUNKID_REF_VARIABLES );
		WRITE_MICRO_CHUNK( csave, MICROCHUNKID_TARGET, ReferenceTarget );
	csave.End_Chunk();
	return true;
}


bool	ReferencerClass::Load( ChunkLoadClass & cload )
{
	cload.Open_Chunk();
	WWASSERT( cload.Cur_Chunk_ID() == CHUNKID_REF_VARIABLES );

	WWASSERT( ReferenceTarget == NULL );
	WWASSERT( TargetReferencerListNext == NULL );

	while (cload.Open_Micro_Chunk()) {
		switch(cload.Cur_Micro_Chunk_ID()) {
			READ_MICRO_CHUNK( cload, MICROCHUNKID_TARGET, ReferenceTarget );
			default:
//				Debug_Say(( "Unrecognized REFLIST Variable chunkID\n" ));
				break;
		}
		cload.Close_Micro_Chunk();
	}
	cload.Close_Chunk();

	if ( ReferenceTarget != NULL ) {
		REQUEST_POINTER_REMAP( (void **)&ReferenceTarget );
	}

	SaveLoadSystemClass::Register_Post_Load_Callback( this );

	return true;
}


void	ReferencerClass::On_Post_Load(void)	
{
	// if we found our target, re-link to it.  
	if ( ReferenceTarget ) {
		ScriptableGameObj* data = ReferenceTarget->Get_Data();
		ReferenceTarget = NULL;
		*this = data;
	}
}


const ReferencerClass & ReferencerClass::operator = ( const ScriptableGameObj * reference_target )
{
	if ( ReferenceTarget != NULL ) {		// if I currently have a target

		ReferencerClass *referencer = ReferenceTarget->ReferencerListHead;
		WWASSERT( referencer );

		if ( referencer == this ) {	// if I'm the first in the list, fix the head
			ReferenceTarget->ReferencerListHead = TargetReferencerListNext;
			TargetReferencerListNext = NULL;
			ReferenceTarget = NULL;
		} else {
			WWASSERT( referencer->TargetReferencerListNext );

			while ( ReferenceTarget != NULL ) {	// Find me in the list, and remove me
				if ( referencer->TargetReferencerListNext == this ) {
					referencer->TargetReferencerListNext = TargetReferencerListNext;
					TargetReferencerListNext = NULL;
					ReferenceTarget = NULL;
				} else {
					referencer = referencer->TargetReferencerListNext;
					WWASSERT( referencer != NULL );
				}
			}
		}
	}

	WWASSERT( ReferenceTarget == NULL );

	if ( reference_target != NULL ) {			// if new reference is non-null
		ReferenceTarget = (ReferenceableClass<ScriptableGameObj> *) (reference_target);		// set it and link list
		TargetReferencerListNext = ReferenceTarget->ReferencerListHead;
		ReferenceTarget->ReferencerListHead = this;
	}

	return *this;
}
