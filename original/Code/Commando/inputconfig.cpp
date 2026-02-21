#include "inputconfig.h"
#include "chunkio.h"
////////////////////////////////////////////////////////////////
//	Save/load constants
////////////////////////////////////////////////////////////////
enum
{
	CHUNKID_VARIABLES			= 0x07181106,
};

enum
{
	VARID_DISPLAY_NAME			= 1,
	VARID_FILENAME,
	VARID_IS_DEFAULT,
	VARID_IS_CUSTOM,
};

////////////////////////////////////////////////////////////////
//
//	Save
//
////////////////////////////////////////////////////////////////
void
InputConfigClass::Save (ChunkSaveClass &csave)
{
	//
	//	Save the variables to their own chunk
	//
	csave.Begin_Chunk (CHUNKID_VARIABLES);
		WRITE_MICRO_CHUNK_WIDESTRING	(csave, VARID_DISPLAY_NAME,	DisplayName);
		WRITE_MICRO_CHUNK_WWSTRING		(csave, VARID_FILENAME,			Filename);
		WRITE_MICRO_CHUNK					(csave, VARID_IS_DEFAULT,		IsDefault);
		WRITE_MICRO_CHUNK					(csave, VARID_IS_CUSTOM,		IsCustom);
	csave.End_Chunk ();
	return ;
}

////////////////////////////////////////////////////////////////
//
//	Load
//
////////////////////////////////////////////////////////////////
void
InputConfigClass::Load (ChunkLoadClass &cload)
{
	//
	//	Read all the sub-chunks
	//
	while (cload.Open_Chunk ()) {
		switch (cload.Cur_Chunk_ID ()) {

			case CHUNKID_VARIABLES:
				Load_Variables (cload);
				break;

			default:
				Debug_Say (("Unrecognized InputConfigClass::Load Chunk ID\n"));
				break;
		}

		cload.Close_Chunk ();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	Load_Variables
//
////////////////////////////////////////////////////////////////
void
InputConfigClass::Load_Variables (ChunkLoadClass &cload)
{
	//
	//	Read all the microchunks
	//
	while (cload.Open_Micro_Chunk ()) {
		switch(cload.Cur_Micro_Chunk_ID ()) {			

			READ_MICRO_CHUNK_WIDESTRING	(cload, VARID_DISPLAY_NAME,	DisplayName);
			READ_MICRO_CHUNK_WWSTRING		(cload, VARID_FILENAME,			Filename);
			READ_MICRO_CHUNK					(cload, VARID_IS_DEFAULT,		IsDefault);
			READ_MICRO_CHUNK					(cload, VARID_IS_CUSTOM,		IsCustom);
		}

		cload.Close_Micro_Chunk ();
	}

	return ;
}

////////////////////////////////////////////////////////////////
//
//	operator=
//
////////////////////////////////////////////////////////////////
const InputConfigClass &
InputConfigClass::operator= (const InputConfigClass &src)
{
	if (&src != this) {
		DisplayName	= src.DisplayName;
		Filename		= src.Filename;
		IsDefault	= src.IsDefault;
		IsCustom		= src.IsCustom;
	}

	return *this;
}
