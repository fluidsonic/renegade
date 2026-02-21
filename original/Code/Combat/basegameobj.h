#ifndef	BASEGAMEOBJ_H
#define	BASEGAMEOBJ_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef PERSIST_H
	#include "persist.h"
#endif

#ifndef DEFINITION_H
	#include "definition.h"
#endif

#ifndef __NETWORKOBJECT_H
	#include "networkobject.h"
#endif

#include "netclassids.h"

/*
**
*/
class	SmartGameObj;
class	PhysicalGameObj;
class	ScriptableGameObj;
class VehicleGameObj;

/*
** BaseGameObjDef - Defintion class for a BaseGameObj
*/
class BaseGameObjDef : public DefinitionClass
{
public:
	virtual bool								Save( ChunkSaveClass &csave );
	virtual bool								Load( ChunkLoadClass &cload );
};

/*
**
*/
class BaseGameObj : public PersistClass, public NetworkObjectClass {

public:
	//	Constructor and Destructor
	BaseGameObj( void );
	virtual	~BaseGameObj( void );

	// Definitions
	virtual	void					Init( void )											= 0;
	void								Init( const BaseGameObjDef & definition );
	const BaseGameObjDef &		Get_Definition( void ) const ;

	// Save / Load
	virtual	bool					Save( ChunkSaveClass & csave );
	virtual	bool					Load( ChunkLoadClass & cload );

	//	Thinking
	virtual	void					Think()				{ IsPostThinkAllowed = true; }
	virtual	void					Post_Think()		{};

	// ID
	void	Set_ID( int id )				{ Set_Network_ID (id); }
	int	Get_ID( void )	const			{ return Get_Network_ID (); }

	// Hibernation
	virtual	bool	Is_Hibernating( void )	{ return false; }

	// Termination
	//virtual	void					Destroy(bool damaged = false)	{ DestroyType = damaged ? DESTROY_DAMAGED : DESTROY_CONTROLLED; }
	//bool								Is_Destroy()						{ return (DestroyType != DESTROY_NONE); }
	//bool								Is_Damage_Destroyed()			{ return (DestroyType == DESTROY_DAMAGED); }

	// Type identification
	virtual	PhysicalGameObj	*As_PhysicalGameObj( void )	{ return (PhysicalGameObj*)NULL; };
	virtual  VehicleGameObj		*As_VehicleGameObj( void )		{ return (VehicleGameObj *)NULL; }
	virtual	SmartGameObj		*As_SmartGameObj( void )		{ return (SmartGameObj*)NULL; };
	virtual	ScriptableGameObj	*As_ScriptableGameObj( void )	{ return (ScriptableGameObj*)NULL; };

	// Network support
	virtual uint32					Get_Network_Class_ID( void ) const		{ return NETCLASSID_GAMEOBJ; }
	virtual void					Delete (void)									{ delete this; }

	bool								Is_Post_Think_Allowed( void )				{ return IsPostThinkAllowed; }

	void								Enable_Cinematic_Freeze( bool enable )	{ EnableCinematicFreeze = enable; }
	bool								Is_Cinematic_Freeze_Enabled( void )		{ return EnableCinematicFreeze; }

private:

	// Constants
	/*enum
	{
		DESTROY_NONE			= 0,
		DESTROY_DAMAGED,
		DESTROY_CONTROLLED
	};*/

	// Member data
	const BaseGameObjDef *	Definition;	
	//int							DestroyType;
	//int							ID;

	// This is used to prevent postthinking before a think call
	bool							IsPostThinkAllowed;

	// This keeps certain object alive during cinematic freeze
	bool							EnableCinematicFreeze;
};

#endif	//	BASEGAMEOBJ_H

