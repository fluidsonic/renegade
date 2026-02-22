#pragma once

#include "global.h"


#include "saveloadsubsystem.h"

#include "combatchunkid.h"

/*
**
*/
class CombatSaveLoadClass : public SaveLoadSubSystemClass {

public:
	CombatSaveLoadClass(void) {}
	virtual ~CombatSaveLoadClass(void) {}

	virtual uint32_t	Chunk_ID (void) const { return CHUNKID_COMBAT; }

protected:
	virtual bool	Save( ChunkSaveClass &csave );
	virtual bool	Load( ChunkLoadClass &cload );
	virtual const char*		Name() const { return "CombatSaveLoadClass"; }
	virtual void	On_Post_Load (void);

};

/*
**
*/
extern	CombatSaveLoadClass	_CombatSaveLoad;
