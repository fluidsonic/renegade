#pragma once

#include "global.h"


#include "saveloadsubsystem.h"

#include "commandochunkids.h"

/*
**
*/
class CommandoSaveLoadClass : public SaveLoadSubSystemClass {

public:
	CommandoSaveLoadClass(void) {}
	virtual ~CommandoSaveLoadClass(void) {}

	virtual uint32_t	Chunk_ID (void) const { return CHUNKID_COMMANDO; }

protected:
	virtual bool	Save( ChunkSaveClass &csave );
	virtual bool	Load( ChunkLoadClass &cload );
	virtual const char* Name() const { return "CommandoSaveLoadClass"; }
};

/*
**
*/
extern	CommandoSaveLoadClass	_CommandoSaveLoad;
