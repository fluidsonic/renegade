#ifndef	SCRIPTS_H
#define	SCRIPTS_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	SCRIPTEVENTS_H
	#include "scriptevents.h"
#endif

#ifndef	SIMPLEVEC_H
	#include "simplevec.h"
#endif

/*
**
*/
class		PhysicalGameObj;
class		ChunkSaveClass;
class		ChunkLoadClass;

#define	SCRIPT_MAX_PARAMS		100

/*
**
*/
class		ScriptSaver	{
public:
	ScriptSaver( ChunkSaveClass & csave	) : CSave( csave ) {}
	operator ChunkSaveClass	& (void) const { return CSave; }
	ChunkSaveClass	& CSave;
};

class		ScriptLoader	{
public:
	ScriptLoader( ChunkLoadClass & cload	) : CLoad( cload ) {}
	operator ChunkLoadClass	& (void) const { return CLoad; }
	ChunkLoadClass	& CLoad;
};

/*
** Script Manager
*/
class	ScriptManager {

public:
	static	void				Init( void );
	static	void				Shutdown( void );

	// Create a script.  Add to the active list
	static	ScriptClass	*	Create_Script( const char * script_name );
	// Add Script to the Destroy List
	static	void				Request_Destroy_Script( ScriptClass * script );
	// Destroy scripts from the destroy list
	static	void				Destroy_Pending( void );

	// Save & load all active scripts
	static	bool				Save( ChunkSaveClass & csave );
	static	bool				Load( ChunkLoadClass & cload );

	static	void				Enable_Script_Creation( bool enable ) { EnableScriptCreation = enable; }

private:
	static void Load_Scripts(const char * dll_filename);

	static LPFN_CREATE_SCRIPT ScriptCreateFunct;
	static LPFN_DESTROY_SCRIPT ScriptDestroyFunct;

	static SimpleDynVecClass<ScriptClass *> ActiveScriptList;
	static SimpleDynVecClass<ScriptClass *> PendingDestroyList;

	static bool	EnableScriptCreation;
};

#endif		// SCRIPTS_H